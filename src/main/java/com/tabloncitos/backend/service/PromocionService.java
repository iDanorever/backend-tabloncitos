package com.tabloncitos.backend.service;

import com.tabloncitos.backend.dto.PromocionRequest;
import com.tabloncitos.backend.dto.PromocionResponse;
import com.tabloncitos.backend.model.Producto;
import com.tabloncitos.backend.model.Promocion;
import com.tabloncitos.backend.repository.ProductoRepository;
import com.tabloncitos.backend.repository.PromocionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PromocionService {

    private static final Logger log = LoggerFactory.getLogger(PromocionService.class);
    private static final ZoneId ZONA_RESTAURANTE = ZoneId.of("America/Lima");
    private static final Set<String> TIPOS = Set.of("PORCENTAJE", "PRECIO_FIJO");
    private static final Set<String> DIAS = Set.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
    private static final Set<String> CANALES = Set.of("SALON", "DELIVERY", "TAKEOUT");

    private final PromocionRepository promocionRepository;
    private final ProductoRepository productoRepository;

    public PromocionService(PromocionRepository promocionRepository, ProductoRepository productoRepository) {
        this.promocionRepository = promocionRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public List<PromocionResponse> listar() {
        sincronizarPrecios(LocalDateTime.now(ZONA_RESTAURANTE));
        return promocionRepository.findAllByOrderByIdDesc().stream().map(this::respuesta).toList();
    }

    @Transactional
    public PromocionResponse guardar(PromocionRequest request, Long id) {
        if (request == null) throw new IllegalArgumentException("Ingrese los datos de la promoción");
        Promocion existente = id == null ? null : buscar(id);
        Producto producto = productoRepository.findById(request.productoId() == null ? -1L : request.productoId())
                .orElseThrow(() -> new IllegalArgumentException("El plato seleccionado no existe"));
        BigDecimal precioBase = existente != null
                && existente.getProducto().getId().equals(producto.getId())
                ? existente.getPrecioOriginal()
                : producto.getPrecio();
        DatosValidados datos = validar(request, precioBase);

        Promocion promocion;
        if (id == null) {
            promocion = new Promocion();
            promocion.setPrecioOriginal(precioBase);
        } else {
            promocion = existente;
            if (!List.of("BORRADOR", "ACTIVA").contains(promocion.getEstado())) {
                throw new IllegalArgumentException("Solo se pueden editar promociones en borrador o activas");
            }
            if ("ACTIVA".equals(promocion.getEstado())) {
                promocion.getProducto().setPrecio(promocion.getPrecioOriginal());
                productoRepository.save(promocion.getProducto());
                promocion.setEstado("BORRADOR");
            }
            if (!promocion.getProducto().getId().equals(producto.getId())) {
                promocion.setPrecioOriginal(producto.getPrecio());
            }
        }

        promocion.setNombre(datos.nombre());
        promocion.setDescripcion(datos.descripcion());
        promocion.setTipoBeneficio(datos.tipoBeneficio());
        promocion.setValorBeneficio(datos.valorBeneficio());
        promocion.setProducto(producto);
        promocion.setDiasSemana(String.join(",", datos.diasSemana()));
        promocion.setHoraInicio(datos.horaInicio());
        promocion.setHoraFin(datos.horaFin());
        promocion.setFechaInicio(datos.fechaInicio());
        promocion.setFechaFin(datos.fechaFin());
        promocion.setCanales(String.join(",", datos.canales()));
        promocion.setEstado("BORRADOR");
        return respuesta(promocionRepository.save(promocion));
    }

    @Transactional
    public PromocionResponse publicar(Long id) {
        Promocion promocion = buscar(id);
        if (!List.of("BORRADOR", "ACTIVA").contains(promocion.getEstado())) {
            throw new IllegalArgumentException("La promoción archivada o vencida no se puede publicar");
        }

        Producto producto = promocion.getProducto();
        if (promocionRepository.existsByProductoIdAndEstado(producto.getId(), "ACTIVA")
                && !"ACTIVA".equals(promocion.getEstado())) {
            throw new IllegalArgumentException("Ya existe una promoción activa para este plato");
        }

        promocion.setEstado("ACTIVA");
        Promocion guardada = promocionRepository.save(promocion);
        sincronizarPrecios(LocalDateTime.now(ZONA_RESTAURANTE));
        return respuesta(guardada);
    }

    @Transactional
    public PromocionResponse archivar(Long id) {
        Promocion promocion = buscar(id);
        restaurarPrecio(promocion);
        promocion.setEstado("ARCHIVADA");
        return respuesta(promocionRepository.save(promocion));
    }

    @Transactional
    public void sincronizarPrecios() {
        sincronizarPrecios(LocalDateTime.now(ZONA_RESTAURANTE));
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void revisarVigencias() {
        try {
            sincronizarPrecios(LocalDateTime.now(ZONA_RESTAURANTE));
        } catch (RuntimeException error) {
            log.error("No se pudieron sincronizar precios promocionales", error);
        }
    }

    private void sincronizarPrecios(LocalDateTime ahora) {
        for (Promocion promocion : promocionRepository.findByEstado("ACTIVA")) {
            if (ahora.toLocalDate().isAfter(promocion.getFechaFin())) {
                restaurarPrecio(promocion);
                promocion.setEstado("VENCIDA");
                promocionRepository.save(promocion);
                continue;
            }

            boolean dentroDeVigencia = !ahora.toLocalDate().isBefore(promocion.getFechaInicio());
            boolean salonHabilitado = contiene(promocion.getCanales(), "SALON");
            boolean dentroDelHorario = aplicaEnHorario(promocion, ahora);
            BigDecimal precio = dentroDeVigencia && salonHabilitado && dentroDelHorario
                    ? calcularPrecio(promocion)
                    : promocion.getPrecioOriginal();
            Producto producto = promocion.getProducto();
            if (producto.getActivo() && producto.getPrecio().compareTo(precio) != 0) {
                producto.setPrecio(precio);
                productoRepository.save(producto);
            }
        }
    }

    private boolean aplicaEnHorario(Promocion promocion, LocalDateTime ahora) {
        LocalTime hora = ahora.toLocalTime();
        DayOfWeek dia = ahora.getDayOfWeek();
        if (promocion.getHoraFin().isAfter(promocion.getHoraInicio())
                || promocion.getHoraFin().equals(promocion.getHoraInicio())) {
            return contiene(promocion.getDiasSemana(), dia.name())
                    && !hora.isBefore(promocion.getHoraInicio())
                    && !hora.isAfter(promocion.getHoraFin());
        }
        if (!hora.isBefore(promocion.getHoraInicio())) {
            return contiene(promocion.getDiasSemana(), dia.name());
        }
        return !hora.isAfter(promocion.getHoraFin())
                && contiene(promocion.getDiasSemana(), dia.minus(1).name());
    }

    private BigDecimal calcularPrecio(Promocion promocion) {
        if ("PRECIO_FIJO".equals(promocion.getTipoBeneficio())) {
            return promocion.getValorBeneficio().setScale(2, RoundingMode.HALF_UP);
        }
        return promocion.getPrecioOriginal()
                .multiply(BigDecimal.ONE.subtract(promocion.getValorBeneficio()
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void restaurarPrecio(Promocion promocion) {
        Producto producto = promocion.getProducto();
        if (producto.getActivo() && producto.getPrecio().compareTo(promocion.getPrecioOriginal()) != 0) {
            producto.setPrecio(promocion.getPrecioOriginal());
            productoRepository.save(producto);
        }
    }

    private Promocion buscar(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La promoción no existe"));
    }

    private DatosValidados validar(PromocionRequest request, BigDecimal precioOriginal) {
        if (request == null) throw new IllegalArgumentException("Ingrese los datos de la promoción");
        String nombre = request.nombre() == null ? "" : request.nombre().trim();
        if (nombre.isBlank() || nombre.length() > 120) {
            throw new IllegalArgumentException("El nombre es obligatorio y no debe superar 120 caracteres");
        }
        String tipo = request.tipoBeneficio() == null
                ? ""
                : request.tipoBeneficio().trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(tipo)) throw new IllegalArgumentException("Seleccione un tipo de beneficio válido");
        if (request.valorBeneficio() == null
                || request.valorBeneficio().signum() <= 0
                || request.valorBeneficio().compareTo(new BigDecimal("99999999.99")) > 0
                || request.valorBeneficio().stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El beneficio debe ser mayor que cero");
        }
        if ("PORCENTAJE".equals(tipo) && request.valorBeneficio().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El descuento no puede superar el 100%");
        }
        if (request.productoId() == null || request.productoId() <= 0) {
            throw new IllegalArgumentException("Seleccione un plato");
        }
        if (request.diasSemana() == null || request.diasSemana().isEmpty()
                || request.diasSemana().stream().anyMatch(dia -> !DIAS.contains(dia))) {
            throw new IllegalArgumentException("Seleccione al menos un día válido");
        }
        if (request.horaInicio() == null || request.horaFin() == null) {
            throw new IllegalArgumentException("Configure el horario de inicio y fin");
        }
        if (request.fechaInicio() == null || request.fechaFin() == null
                || request.fechaFin().isBefore(request.fechaInicio())) {
            throw new IllegalArgumentException("Configure un rango de fechas válido");
        }
        if (request.canales() == null || request.canales().isEmpty()
                || request.canales().stream().anyMatch(canal -> !CANALES.contains(canal))) {
            throw new IllegalArgumentException("Seleccione al menos un canal válido");
        }
        String descripcion = request.descripcion() == null ? "" : request.descripcion().trim();
        if (descripcion.length() > 500) throw new IllegalArgumentException("La descripción no debe superar 500 caracteres");

        if ("PRECIO_FIJO".equals(tipo) && request.valorBeneficio().compareTo(precioOriginal) > 0) {
            throw new IllegalArgumentException("El precio promocional no puede superar el precio actual del plato");
        }

        return new DatosValidados(
                nombre,
                descripcion,
                tipo,
                request.valorBeneficio().setScale(2, RoundingMode.HALF_UP),
                request.productoId(),
                request.diasSemana().stream().distinct().toList(),
                request.horaInicio(),
                request.horaFin(),
                request.fechaInicio(),
                request.fechaFin(),
                request.canales().stream().distinct().toList());
    }

    private PromocionResponse respuesta(Promocion promocion) {
        return new PromocionResponse(
                promocion.getId(),
                promocion.getNombre(),
                promocion.getDescripcion(),
                promocion.getTipoBeneficio(),
                promocion.getValorBeneficio(),
                promocion.getProducto().getId(),
                promocion.getProducto().getNombre(),
                promocion.getPrecioOriginal(),
                calcularPrecio(promocion),
                Arrays.asList(promocion.getDiasSemana().split(",")),
                promocion.getHoraInicio(),
                promocion.getHoraFin(),
                promocion.getFechaInicio(),
                promocion.getFechaFin(),
                Arrays.asList(promocion.getCanales().split(",")),
                promocion.getEstado());
    }

    private boolean contiene(String csv, String valor) {
        return Arrays.asList(csv.split(",")).contains(valor);
    }

    private record DatosValidados(
            String nombre,
            String descripcion,
            String tipoBeneficio,
            BigDecimal valorBeneficio,
            Long productoId,
            List<String> diasSemana,
            LocalTime horaInicio,
            LocalTime horaFin,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            List<String> canales) {
    }
}
