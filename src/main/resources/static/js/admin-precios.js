(() => {
    const selectorProductos = document.getElementById('promotion-product');
    const busquedaProductos = document.getElementById('promotion-product-search');
    const sinResultadosProductos = document.getElementById('promotion-product-search-empty');
    const listaPromociones = document.getElementById('promotion-list');
    const contadorPromociones = document.getElementById('promotion-count');
    const editor = document.querySelector('#btn-save-rule')?.closest('.rounded-2xl');
    const errorPromocion = document.getElementById('promotion-error');
    const botonGuardarBorrador = document.getElementById('btn-save-draft');
    const botonGuardarPublicar = document.getElementById('btn-save-rule');
    const botonSincronizar = document.getElementById('btn-sync-live');
    const dias = [...document.querySelectorAll('[data-day]')];
    const canales = [...document.querySelectorAll('.promotion-channel')];
    const fechaActual = new Date();
    const fechaLocal = new Date(fechaActual.getTime() - fechaActual.getTimezoneOffset() * 60000)
        .toISOString().slice(0, 10);
    const formatoPrecio = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });
    const nombresDias = {
        MONDAY: 'Lunes',
        TUESDAY: 'Martes',
        WEDNESDAY: 'Miércoles',
        THURSDAY: 'Jueves',
        FRIDAY: 'Viernes',
        SATURDAY: 'Sábado',
        SUNDAY: 'Domingo'
    };
    const nombresCanales = { SALON: 'Salón', DELIVERY: 'Delivery', TAKEOUT: 'Para llevar' };
    const estados = {
        ACTIVA: ['Activa', 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30'],
        BORRADOR: ['Borrador', 'bg-surface-container-high text-on-surface-variant border-surface-container-highest'],
        VENCIDA: ['Vencida', 'bg-secondary/15 text-secondary border-secondary/30'],
        ARCHIVADA: ['Archivada', 'bg-surface-container-high text-on-surface-variant border-surface-container-highest']
    };

    let productos = [];
    let promociones = [];
    let promocionSeleccionadaId = null;
    let busquedaPromociones = '';

    function escaparHtml(valor) {
        return String(valor ?? '').replace(/[&<>"']/g, caracter => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        })[caracter]);
    }

    async function solicitar(url, opciones = {}) {
        const respuesta = await fetch(url, { cache: 'no-store', ...opciones });
        const datos = await respuesta.json().catch(() => ({}));
        if (!respuesta.ok) throw new Error(datos.mensaje || datos.error || `Error HTTP ${respuesta.status}`);
        return datos;
    }

    function aviso(mensaje, error = false) {
        window.tabloncitosAdmin?.mostrarAviso(mensaje, error);
    }

    function mostrarError(elemento, mensaje) {
        elemento.textContent = mensaje;
        elemento.classList.remove('hidden');
    }

    function ocultarError(elemento) {
        elemento.textContent = '';
        elemento.classList.add('hidden');
    }

    function productoPorId(id) {
        return productos.find(producto => String(producto.id) === String(id));
    }

    function actualizarVistaPrevia() {
        const producto = productoPorId(selectorProductos.value);
        const promocionActual = promociones.find(promo => promo.id === promocionSeleccionadaId);
        const precioBase = Number(promocionActual?.productoId === Number(selectorProductos.value)
            ? promocionActual.precioOriginal
            : producto?.precio);
        const beneficio = Number(document.getElementById('promotion-benefit-value').value);
        const tipo = document.getElementById('promotion-benefit-type').value;
        const precioPromo = Number.isFinite(precioBase) && Number.isFinite(beneficio)
            ? tipo === 'PRECIO_FIJO' ? beneficio : precioBase * (1 - beneficio / 100)
            : NaN;

        document.getElementById('promotion-original-price').textContent =
            Number.isFinite(precioBase) ? formatoPrecio.format(precioBase) : '—';
        document.getElementById('promotion-product-name').textContent = producto?.nombre || 'Selecciona un plato';
        document.getElementById('promotion-final-price').textContent =
            Number.isFinite(precioPromo) ? formatoPrecio.format(precioPromo) : '—';
        document.getElementById('promotion-saving').textContent =
            Number.isFinite(precioPromo) && Number.isFinite(precioBase)
                ? `Ahorro: ${formatoPrecio.format(Math.max(0, precioBase - precioPromo))}`
                : 'Define el beneficio';

        const inicio = document.getElementById('promotion-date-start').value;
        const fin = document.getElementById('promotion-date-end').value;
        document.getElementById('promotion-preview-dates').textContent =
            inicio && fin ? `${inicio} — ${fin}` : 'Selecciona fechas';
        const horaInicio = document.getElementById('promotion-time-start').value;
        const horaFin = document.getElementById('promotion-time-end').value;
        document.getElementById('promotion-preview-hours').textContent =
            horaInicio && horaFin ? `${horaInicio} — ${horaFin}` : 'Selecciona horario';
        const diasElegidos = dias.filter(dia => dia.getAttribute('aria-pressed') === 'true')
            .map(dia => nombresDias[dia.dataset.day]);
        document.getElementById('promotion-days-label').textContent =
            diasElegidos.length ? diasElegidos.join(', ') : 'Elige al menos un día';
    }

    function renderizarOpcionesProductos() {
        const consulta = busquedaProductos.value.trim().toLocaleLowerCase('es');
        const filtrados = productos.filter(producto =>
            `${producto.nombre} ${producto.categoria?.nombre || ''} ${producto.estacionKds || ''}`
                .toLocaleLowerCase('es').includes(consulta));
        const promocionActual = promociones.find(promo => promo.id === promocionSeleccionadaId);
        const seleccionActual = selectorProductos.value || String(promocionActual?.productoId || '');
        const productoSeleccionado = productoPorId(seleccionActual);
        const opciones = productoSeleccionado && !filtrados.includes(productoSeleccionado)
            ? [productoSeleccionado, ...filtrados]
            : filtrados;

        selectorProductos.replaceChildren(new Option('Selecciona un plato', ''));
        opciones.forEach(producto => {
            selectorProductos.add(new Option(`${producto.nombre} — ${formatoPrecio.format(Number(producto.precio))}`, producto.id));
        });
        sinResultadosProductos.classList.toggle('hidden', !consulta || filtrados.length > 0);
        if (opciones.some(producto => String(producto.id) === seleccionActual)) {
            selectorProductos.value = seleccionActual;
        }
        actualizarVistaPrevia();
    }

    async function cargarProductos() {
        const respuesta = await solicitar('/api/productos');
        if (!Array.isArray(respuesta)) throw new Error('El catálogo no tiene un formato válido');
        productos = respuesta;
        renderizarOpcionesProductos();
        return productos;
    }

    function renderizarPromociones() {
        const activas = promociones.filter(promo => promo.estado === 'ACTIVA').length;
        contadorPromociones.textContent = `${activas} activas · ${promociones.length} en total`;
        const filtradas = promociones.filter(promo =>
            `${promo.nombre} ${promo.productoNombre} ${promo.estado}`
                .toLocaleLowerCase('es').includes(busquedaPromociones));

        if (!filtradas.length) {
            listaPromociones.innerHTML = `<p class="rounded-xl border border-surface-container-high bg-surface-container-low p-4 text-sm text-on-surface-variant">${
                promociones.length ? 'No hay promociones que coincidan con el filtro.' : 'Todavía no hay reglas. Crea una promoción para empezar.'
            }</p>`;
            return;
        }

        listaPromociones.innerHTML = filtradas.map(promo => {
            const [estadoNombre, estadoClase] = estados[promo.estado] || estados.BORRADOR;
            const descuento = promo.tipoBeneficio === 'PRECIO_FIJO'
                ? `Precio ${formatoPrecio.format(Number(promo.precioPromocional))}`
                : `${Number(promo.valorBeneficio)}% de descuento`;
            const canalesLabel = (promo.canales || []).map(canal => nombresCanales[canal] || canal).join(' · ');
            return `
                <article class="promotion-card rounded-2xl border ${
                    promo.id === promocionSeleccionadaId ? 'border-primary/60 bg-surface-container-high' : 'border-surface-container-high bg-surface-container-low'
                } p-4" data-promotion-id="${promo.id}">
                    <div class="flex items-start justify-between gap-3">
                        <button class="min-w-0 flex-1 text-left" type="button" data-edit-promotion="${promo.id}">
                            <span class="flex flex-wrap items-center gap-2">
                                <span class="truncate font-bold text-on-surface">${escaparHtml(promo.nombre)}</span>
                                <span class="rounded-full border px-2 py-0.5 text-xs font-semibold ${estadoClase}">${estadoNombre}</span>
                            </span>
                            <span class="mt-1 block text-xs text-on-surface-variant">${escaparHtml(promo.productoNombre)} · ${escaparHtml(descuento)}</span>
                            <span class="mt-2 block text-xs text-on-surface-variant">${escaparHtml((promo.diasSemana || []).map(dia => nombresDias[dia]).join(', '))} · ${escaparHtml(promo.horaInicio)}–${escaparHtml(promo.horaFin)}</span>
                            <span class="mt-1 block text-xs text-on-surface-variant">${escaparHtml(promo.fechaInicio)} — ${escaparHtml(promo.fechaFin)} · ${escaparHtml(canalesLabel)}</span>
                        </button>
                        ${['ACTIVA', 'BORRADOR'].includes(promo.estado)
                            ? `<button class="shrink-0 rounded-lg border border-surface-container-highest px-2.5 py-1.5 text-xs font-semibold text-on-surface-variant hover:bg-surface-container-high" type="button" data-archive-promotion="${promo.id}">Archivar</button>`
                            : ''}
                    </div>
                </article>`;
        }).join('');
    }

    async function cargarPromociones() {
        promociones = await solicitar('/api/promociones');
        if (!Array.isArray(promociones)) throw new Error('La lista de promociones no tiene un formato válido');
        if (promocionSeleccionadaId && !promociones.some(promo => promo.id === promocionSeleccionadaId)) {
            promocionSeleccionadaId = null;
        }
        renderizarPromociones();
        return promociones;
    }

    function actualizarTipoBeneficio() {
        const tipo = document.getElementById('promotion-benefit-type').value;
        const entrada = document.getElementById('promotion-benefit-value');
        const etiqueta = document.getElementById('promotion-benefit-label');
        if (tipo === 'PRECIO_FIJO') {
            etiqueta.textContent = 'Precio promocional (S/)';
            entrada.max = '99999999.99';
            entrada.step = '0.01';
        } else {
            etiqueta.textContent = 'Descuento (%)';
            entrada.max = '100';
            entrada.step = '0.01';
        }
        actualizarVistaPrevia();
    }

    function seleccionarDias(seleccionados = []) {
        dias.forEach(dia => {
            const activo = seleccionados.includes(dia.dataset.day);
            dia.setAttribute('aria-pressed', String(activo));
            dia.classList.toggle('bg-primary', activo);
            dia.classList.toggle('text-on-primary', activo);
            dia.classList.toggle('shadow-sm', activo);
            dia.classList.toggle('bg-surface-container-high', !activo);
            dia.classList.toggle('text-tertiary', !activo);
        });
        actualizarVistaPrevia();
    }

    function resetearEditor() {
        promocionSeleccionadaId = null;
        document.getElementById('promotion-name').value = '';
        document.getElementById('promotion-description').value = '';
        document.getElementById('promotion-benefit-type').value = 'PORCENTAJE';
        document.getElementById('promotion-benefit-value').value = '';
        document.getElementById('promotion-time-start').value = '12:00';
        document.getElementById('promotion-time-end').value = '23:30';
        document.getElementById('promotion-date-start').value = fechaLocal;
        document.getElementById('promotion-date-start').min = fechaLocal;
        document.getElementById('promotion-date-end').value = fechaLocal;
        document.getElementById('promotion-date-end').min = fechaLocal;
        selectorProductos.value = '';
        busquedaProductos.value = '';
        renderizarOpcionesProductos();
        canales.forEach(canal => { canal.checked = canal.dataset.channel === 'SALON'; });
        seleccionarDias([]);
        document.getElementById('promotion-editor-title').textContent = 'Nueva promoción';
        document.getElementById('promotion-editor-status').textContent = 'Borrador';
        document.getElementById('promotion-editor-message').textContent = 'Completa los datos y guarda el borrador o publícalo.';
        ocultarError(errorPromocion);
        renderizarPromociones();
        actualizarTipoBeneficio();
    }

    function cargarEnEditor(promo) {
        promocionSeleccionadaId = promo.id;
        document.getElementById('promotion-name').value = promo.nombre;
        document.getElementById('promotion-description').value = promo.descripcion || '';
        document.getElementById('promotion-benefit-type').value = promo.tipoBeneficio;
        document.getElementById('promotion-benefit-value').value = promo.valorBeneficio;
        selectorProductos.value = String(promo.productoId);
        busquedaProductos.value = '';
        renderizarOpcionesProductos();
        document.getElementById('promotion-time-start').value = promo.horaInicio;
        document.getElementById('promotion-time-end').value = promo.horaFin;
        document.getElementById('promotion-date-start').value = promo.fechaInicio;
        document.getElementById('promotion-date-start').min = fechaLocal;
        document.getElementById('promotion-date-end').value = promo.fechaFin;
        document.getElementById('promotion-date-end').min = promo.fechaInicio;
        canales.forEach(canal => { canal.checked = promo.canales.includes(canal.dataset.channel); });
        seleccionarDias(promo.diasSemana);
        document.getElementById('promotion-editor-title').textContent = `Editar: ${promo.nombre}`;
        document.getElementById('promotion-editor-status').textContent = estados[promo.estado]?.[0] || promo.estado;
        document.getElementById('promotion-editor-message').textContent =
            `${promo.productoNombre} · ${promo.fechaInicio} — ${promo.fechaFin}`;
        ocultarError(errorPromocion);
        renderizarPromociones();
        actualizarTipoBeneficio();
        editor.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    function obtenerPayloadPromocion() {
        const diasSemana = dias
            .filter(dia => dia.getAttribute('aria-pressed') === 'true')
            .map(dia => dia.dataset.day);
        const canalesSeleccionados = canales.filter(canal => canal.checked).map(canal => canal.dataset.channel);
        return {
            nombre: document.getElementById('promotion-name').value.trim(),
            descripcion: document.getElementById('promotion-description').value.trim(),
            tipoBeneficio: document.getElementById('promotion-benefit-type').value,
            valorBeneficio: Number(document.getElementById('promotion-benefit-value').value),
            productoId: Number(selectorProductos.value),
            diasSemana,
            horaInicio: document.getElementById('promotion-time-start').value,
            horaFin: document.getElementById('promotion-time-end').value,
            fechaInicio: document.getElementById('promotion-date-start').value,
            fechaFin: document.getElementById('promotion-date-end').value,
            canales: canalesSeleccionados
        };
    }

    function validarPromocion(payload) {
        if (!payload.nombre) throw new Error('Escribe el nombre de la promoción.');
        if (!payload.productoId) throw new Error('Selecciona el plato al que se aplicará.');
        if (!Number.isFinite(payload.valorBeneficio) || payload.valorBeneficio <= 0) {
            throw new Error('Ingresa un beneficio mayor que cero.');
        }
        if (payload.tipoBeneficio === 'PORCENTAJE' && payload.valorBeneficio > 100) {
            throw new Error('El descuento no puede superar el 100%.');
        }
        if (!payload.diasSemana.length) throw new Error('Selecciona al menos un día de aplicación.');
        if (!payload.canales.length) throw new Error('Selecciona al menos un canal de venta.');
        if (!payload.horaInicio || !payload.horaFin) throw new Error('Define el horario de aplicación.');
        if (!payload.fechaInicio || !payload.fechaFin || payload.fechaFin < payload.fechaInicio) {
            throw new Error('Define fechas de inicio y fin válidas.');
        }
        const producto = productoPorId(payload.productoId);
        const promoActual = promociones.find(promo => promo.id === promocionSeleccionadaId);
        const precioBase = Number(promoActual?.productoId === payload.productoId
            ? promoActual.precioOriginal
            : producto?.precio);
        if (!Number.isFinite(precioBase)) throw new Error('No se pudo determinar el precio base del plato.');
        if (payload.tipoBeneficio === 'PRECIO_FIJO' && payload.valorBeneficio >= precioBase) {
            throw new Error('El precio promocional debe ser menor al precio regular.');
        }
    }

    async function guardarPromocion() {
        ocultarError(errorPromocion);
        let payload;
        try {
            payload = obtenerPayloadPromocion();
            validarPromocion(payload);
        } catch (error) {
            mostrarError(errorPromocion, error.message);
            throw error;
        }

        const url = promocionSeleccionadaId
            ? `/api/promociones/${promocionSeleccionadaId}`
            : '/api/promociones';
        const promo = await solicitar(url, {
            method: promocionSeleccionadaId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        promocionSeleccionadaId = promo.id;
        await cargarPromociones();
        return promo;
    }

    async function publicarPromocion() {
        botonGuardarPublicar.disabled = true;
        botonGuardarPublicar.textContent = 'Publicando…';
        if (botonSincronizar) botonSincronizar.disabled = true;
        try {
            const borrador = await guardarPromocion();
            const publicada = await solicitar(`/api/promociones/${borrador.id}/publicar`, { method: 'POST' });
            await Promise.all([cargarPromociones(), cargarProductos()]);
            document.getElementById('promotion-editor-status').textContent = estados[publicada.estado]?.[0] || publicada.estado;
            aviso(`${publicada.nombre} guardada. El precio promocional se sincronizó con el menú y se restaurará al finalizar.`);
        } catch (error) {
            if (!errorPromocion.textContent) mostrarError(errorPromocion, error.message || 'No se pudo publicar la promoción.');
        } finally {
            botonGuardarPublicar.disabled = false;
            botonGuardarPublicar.innerHTML = '<span class="material-symbols-outlined text-[18px]">check</span><span>Guardar y publicar</span>';
            if (botonSincronizar) botonSincronizar.disabled = false;
        }
    }

    async function archivarPromocion(id) {
        const promo = promociones.find(item => item.id === id);
        if (!promo || !window.confirm(`¿Archivar "${promo.nombre}"? Se restaurará el precio regular del plato.`)) return;
        try {
            await solicitar(`/api/promociones/${id}/archivar`, { method: 'POST' });
            await Promise.all([cargarPromociones(), cargarProductos()]);
            if (promocionSeleccionadaId === id) resetearEditor();
            aviso('Promoción archivada. El precio regular fue restaurado.');
        } catch (error) {
            aviso(error.message || 'No se pudo archivar la promoción.', true);
        }
    }

    async function sincronizarCatalogo() {
        botonSincronizar.disabled = true;
        const contenidoOriginal = botonSincronizar.innerHTML;
        botonSincronizar.innerHTML = '<span class="material-symbols-outlined animate-spin">sync</span><span>Sincronizando…</span>';
        try {
            const hayCambios = document.getElementById('promotion-name').value.trim()
                || selectorProductos.value
                || document.getElementById('promotion-benefit-value').value;
            if (hayCambios) {
                await publicarPromocion();
            } else {
                await Promise.all([cargarPromociones(), cargarProductos()]);
                aviso('Catálogo y promociones activas sincronizados.');
            }
        } catch (error) {
            aviso(error.message || 'No se pudo sincronizar el menú.', true);
        } finally {
            botonSincronizar.disabled = false;
            botonSincronizar.innerHTML = contenidoOriginal;
        }
    }

    busquedaProductos.addEventListener('input', renderizarOpcionesProductos);
    document.getElementById('promotion-benefit-type').addEventListener('change', actualizarTipoBeneficio);
    document.getElementById('promotion-benefit-value').addEventListener('input', actualizarVistaPrevia);
    selectorProductos.addEventListener('change', actualizarVistaPrevia);
    document.getElementById('promotion-date-start').addEventListener('change', event => {
        const fin = document.getElementById('promotion-date-end');
        fin.min = event.currentTarget.value || fechaLocal;
        if (fin.value && fin.value < fin.min) fin.value = fin.min;
        actualizarVistaPrevia();
    });
    document.getElementById('promotion-date-end').addEventListener('change', actualizarVistaPrevia);
    document.getElementById('promotion-time-start').addEventListener('input', actualizarVistaPrevia);
    document.getElementById('promotion-time-end').addEventListener('input', actualizarVistaPrevia);
    dias.forEach(dia => dia.addEventListener('click', () => {
        const activo = dia.getAttribute('aria-pressed') === 'true';
        const nuevaSeleccion = dias.filter(item => item.getAttribute('aria-pressed') === 'true')
            .map(item => item.dataset.day);
        seleccionarDias(activo
            ? nuevaSeleccion.filter(valor => valor !== dia.dataset.day)
            : [...nuevaSeleccion, dia.dataset.day]);
    }));

    listaPromociones.addEventListener('click', evento => {
        const botonEditar = evento.target.closest('[data-edit-promotion]');
        if (botonEditar) {
            const promo = promociones.find(item => item.id === Number(botonEditar.dataset.editPromotion));
            if (promo && ['BORRADOR', 'ACTIVA'].includes(promo.estado)) cargarEnEditor(promo);
            return;
        }
        const botonArchivar = evento.target.closest('[data-archive-promotion]');
        if (botonArchivar) archivarPromocion(Number(botonArchivar.dataset.archivePromotion));
    });

    document.getElementById('btn-new-promotion').addEventListener('click', () => {
        resetearEditor();
        editor.scrollIntoView({ behavior: 'smooth', block: 'start' });
        document.getElementById('promotion-name').focus({ preventScroll: true });
    });
    document.getElementById('btn-reset-promotion').addEventListener('click', () => {
        const promo = promociones.find(item => item.id === promocionSeleccionadaId);
        if (promo) cargarEnEditor(promo);
        else resetearEditor();
    });
    botonGuardarBorrador.addEventListener('click', async () => {
        botonGuardarBorrador.disabled = true;
        try {
            const promo = await guardarPromocion();
            document.getElementById('promotion-editor-status').textContent = 'Borrador';
            aviso(`Borrador "${promo.nombre}" guardado. Aún no cambia el precio del menú.`);
        } catch (error) {
            if (!errorPromocion.textContent) mostrarError(errorPromocion, error.message || 'No se pudo guardar el borrador.');
        } finally {
            botonGuardarBorrador.disabled = false;
        }
    });
    botonGuardarPublicar.addEventListener('click', publicarPromocion);
    botonSincronizar.addEventListener('click', sincronizarCatalogo);
    document.getElementById('btn-sync-live').disabled = false;

    if (!document.getElementById('promotion-search-input')) {
        const campo = document.createElement('input');
        campo.id = 'promotion-search-input';
        campo.type = 'search';
        campo.placeholder = 'Buscar promoción o plato…';
        campo.setAttribute('aria-label', 'Buscar promociones');
        campo.className = 'w-full rounded-lg border border-surface-container-highest bg-surface-container px-3 py-2 text-sm text-on-surface placeholder:text-on-surface-variant';
        listaPromociones.before(campo);
        campo.addEventListener('input', () => {
            busquedaPromociones = campo.value.trim().toLocaleLowerCase('es');
            renderizarPromociones();
        });
    }

    resetearEditor();
    Promise.all([cargarProductos(), cargarPromociones()])
        .catch(error => {
            console.error('No se pudo inicializar la gestión de precios:', error);
            aviso('No se pudo cargar el catálogo o las promociones.', true);
            if (!promociones.length) listaPromociones.innerHTML = '<p class="text-sm text-secondary">No se pudo cargar la lista de promociones.</p>';
        });
})();
