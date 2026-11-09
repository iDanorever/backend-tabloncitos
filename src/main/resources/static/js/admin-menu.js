(() => {
    const listaProductos = document.getElementById('admin-product-list');
    const contadorProductos = document.getElementById('product-count');
    const contadorFiltrados = document.getElementById('catalog-filter-count');
    const busquedaProductos = document.getElementById('product-search-input');
    const filtroCategorias = document.getElementById('category-filters');
    const filtroInactivos = document.getElementById('toggle-soft-delete');
    const sinResultadosProductos = document.getElementById('product-search-empty');
    const errorCatalogo = document.getElementById('catalog-error');
    const modalProducto = document.getElementById('modal-new-product');
    const formularioProducto = document.getElementById('form-new-product');
    const selectorCategorias = document.getElementById('new-product-category');
    const errorProducto = document.getElementById('new-product-error');
    const botonGuardarProducto = document.getElementById('btn-save-product');
    const entradaImagenProducto = document.getElementById('product-image-input');
    const botonGuardarImagen = document.getElementById('btn-save-product-image');
    const textoImagenSeleccionada = document.getElementById('product-image-selection');
    const errorImagenProducto = document.getElementById('product-image-error');
    const imagenDetalle = document.getElementById('product-detail-image');
    const marcadorImagenDetalle = document.getElementById('product-image-placeholder');
    const formatoPrecio = new Intl.NumberFormat('es-PE', { style: 'currency', currency: 'PEN' });
    const estadoDetalle = document.getElementById('product-detail-status');
    let productos = [];
    let categorias = [];
    let categoriaSeleccionada = 'all';
    let productoSeleccionadoId = null;
    let incluirInactivos = false;
    let archivoImagenSeleccionado = null;
    let productoDetalleRenderizadoId = null;

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

    function productosFiltrados() {
        const consulta = busquedaProductos.value.trim().toLocaleLowerCase('es');
        return productos.filter(producto => {
            const visible = incluirInactivos || producto.activo;
            const coincideTexto = `${producto.id} ${producto.nombre} ${producto.categoria?.nombre || ''} ${producto.estacionKds || ''}`
                .toLocaleLowerCase('es').includes(consulta);
            const coincideCategoria = categoriaSeleccionada === 'all'
                || String(producto.categoria?.id) === categoriaSeleccionada;
            return visible && coincideTexto && coincideCategoria;
        });
    }

    function renderizarCategorias() {
        const opciones = [{ id: 'all', nombre: 'Todas' }, ...categorias];
        filtroCategorias.innerHTML = opciones.map(categoria => {
            const seleccionada = String(categoria.id) === categoriaSeleccionada;
            return `<button class="shrink-0 rounded-lg px-4 py-1.5 text-xs font-bold transition-all ${
                seleccionada
                    ? 'bg-terracota text-white shadow-md'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
            }" type="button" data-category-filter="${escaparHtml(categoria.id)}" aria-pressed="${seleccionada}">${escaparHtml(categoria.nombre)}</button>`;
        }).join('');
    }

    function renderizarDetalle() {
        const producto = productoPorId(productoSeleccionadoId);
        const productoId = producto ? String(producto.id) : null;
        if (productoId !== productoDetalleRenderizadoId) {
            archivoImagenSeleccionado = null;
            entradaImagenProducto.value = '';
            ocultarError(errorImagenProducto);
            productoDetalleRenderizadoId = productoId;
        }
        document.getElementById('product-detail-id').textContent =
            producto ? `PLATO #${producto.id}` : 'Selecciona un plato';
        document.getElementById('product-detail-category').textContent =
            producto ? `Categoría: ${producto.categoria?.nombre || 'Sin categoría'}` : 'Catálogo';
        document.getElementById('product-detail-title').textContent =
            producto?.nombre || 'Detalle del plato';
        document.getElementById('product-detail-price').textContent =
            producto ? formatoPrecio.format(Number(producto.precio)) : '—';
        document.getElementById('product-detail-station').textContent =
            producto?.estacionKds || (producto ? 'Sin asignar' : '—');
        document.getElementById('product-detail-availability').textContent =
            producto ? (producto.activo ? 'Disponible para el mesero' : 'Pausado del menú del mesero')
                : 'Selecciona un plato para ver su estado.';
        estadoDetalle.textContent = producto ? (producto.activo ? 'ACTIVO EN MENÚ' : 'PAUSADO') : '';
        estadoDetalle.className = producto
            ? `rounded border px-2 py-0.5 text-[10px] font-bold ${
                producto.activo
                    ? 'border-emerald-800/60 bg-emerald-950/80 text-emerald-400'
                    : 'border-amber-800/60 bg-amber-950/50 text-amber-300'
            }`
            : 'hidden';
        entradaImagenProducto.disabled = !producto;
        botonGuardarImagen.disabled = !producto || !archivoImagenSeleccionado;
        if (archivoImagenSeleccionado) {
            textoImagenSeleccionada.textContent = `Imagen seleccionada: ${archivoImagenSeleccionado.name}`;
        } else {
            textoImagenSeleccionada.textContent = producto
                ? (producto.tieneImagen ? 'Este plato ya tiene una imagen. Puedes reemplazarla.' : 'Este plato todavía no tiene imagen.')
                : 'Selecciona un plato y una imagen para agregarla.';
        }
        imagenDetalle.classList.toggle('hidden', !producto?.tieneImagen);
        marcadorImagenDetalle.classList.toggle('hidden', Boolean(producto?.tieneImagen));
        if (producto?.tieneImagen) {
            imagenDetalle.src = `/images/productos/${producto.id}.jpg?v=${Date.now()}`;
            imagenDetalle.alt = `Imagen de ${producto.nombre}`;
        } else {
            imagenDetalle.removeAttribute('src');
        }
    }

    function renderizarProductos() {
        const filtrados = productosFiltrados();
        if (filtrados.length && !filtrados.some(producto => String(producto.id) === String(productoSeleccionadoId))) {
            productoSeleccionadoId = filtrados[0].id;
        } else if (!filtrados.length) {
            productoSeleccionadoId = null;
        }

        listaProductos.innerHTML = filtrados.map(producto => {
            const seleccionado = String(producto.id) === String(productoSeleccionadoId);
            const activo = Boolean(producto.activo);
            return `
                <article class="group w-full rounded-xl border p-4 shadow-md transition-all ${
                    seleccionado
                        ? 'border-terracota/80 bg-surface-container-wood ring-1 ring-terracota/40'
                        : 'border-outline-variant/40 bg-surface-container-wood hover:border-outline'
                }">
                    <button class="block w-full text-left" type="button" data-product-id="${producto.id}" aria-pressed="${seleccionado}">
                    <span class="flex items-start gap-4">
                        <span class="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg border border-outline-variant/40 bg-[#181716] text-primary">
                            ${producto.tieneImagen
                                ? `<img class="h-full w-full rounded-lg object-cover" src="/images/productos/${producto.id}.jpg?v=${Date.now()}" alt="" loading="lazy"/>`
                                : `<span class="material-symbols-outlined">${activo ? 'restaurant' : 'pause_circle'}</span>`}
                        </span>
                        <span class="min-w-0 flex-1">
                            <span class="flex items-center justify-between gap-2">
                                <span class="font-mono text-xs font-bold ${activo ? 'text-primary' : 'text-on-surface-variant'}">PLATO #${producto.id}</span>
                                <span class="rounded-md border px-2 py-1 text-[10px] font-extrabold ${
                                    activo
                                        ? 'border-emerald-800/60 bg-[#181716] text-emerald-400'
                                        : 'border-amber-800/60 bg-amber-950/50 text-amber-300'
                                }">${activo ? 'DISPONIBLE' : 'PAUSADO'}</span>
                            </span>
                            <span class="mt-1 block truncate text-sm font-extrabold text-on-surface">${escaparHtml(producto.nombre)}</span>
                            <span class="mt-1 block truncate text-xs text-on-surface-variant">${escaparHtml(producto.categoria?.nombre || 'Sin categoría')} · ${escaparHtml(producto.estacionKds || 'Estación sin asignar')}</span>
                            <span class="mt-3 flex items-center justify-between border-t border-outline-variant/30 bg-[#181716]/60 p-2">
                                <span class="text-[10px] font-bold uppercase text-on-surface-variant">Precio regular</span>
                                <span class="font-mono text-sm font-black ${activo ? 'text-primary' : 'text-on-surface-variant'}">${formatoPrecio.format(Number(producto.precio))}</span>
                            </span>
                        </span>
                    </span>
                    </button>
                    <span class="mt-3 flex items-center justify-between gap-3 border-t border-outline-variant/20 pt-3" data-availability-control>
                        <span class="text-[11px] text-on-surface-variant">${activo ? 'Visible en el menú del mesero' : 'No aparece al tomar pedidos'}</span>
                        <label class="flex cursor-pointer items-center gap-2" title="Cambiar disponibilidad">
                            <span class="text-[10px] font-bold uppercase ${activo ? 'text-emerald-400' : 'text-amber-300'}">${activo ? 'Activo' : 'Pausado'}</span>
                            <input class="sr-only peer" type="checkbox" data-product-availability="${producto.id}" ${activo ? 'checked' : ''} aria-label="${activo ? 'Pausar' : 'Activar'} ${escaparHtml(producto.nombre)}"/>
                            <span class="relative h-4 w-7 rounded-full bg-surface-variant after:absolute after:left-0.5 after:top-0.5 after:h-3 after:w-3 after:rounded-full after:bg-white after:transition-transform peer-checked:bg-emerald-500 peer-checked:after:translate-x-3"></span>
                        </label>
                    </span>
                </article>`;
        }).join('');

        const activos = productos.filter(producto => producto.activo).length;
        const pausados = productos.length - activos;
        const textoConteo = `${filtrados.length} ${filtrados.length === 1 ? 'plato' : 'platos'}`;
        contadorFiltrados.textContent = textoConteo;
        contadorProductos.textContent = `${activos} activos · ${pausados} pausados`;
        const contadorPausados = document.getElementById('paused-product-count');
        if (contadorPausados) {
            contadorPausados.textContent = `${pausados} pausados`;
        }
        const hayFiltros = Boolean(busquedaProductos.value.trim()) || categoriaSeleccionada !== 'all';
        sinResultadosProductos.classList.toggle('hidden', !hayFiltros || filtrados.length > 0 || !productos.length);
        if (!filtrados.length) {
            listaProductos.innerHTML = `<p class="rounded-xl border border-outline-variant/40 bg-surface-container-wood p-5 text-sm text-on-surface-variant">${
                productos.length ? 'No hay platos activos con estos filtros. Activa “Ver pausados” para consultar los inhabilitados.' : 'Todavía no hay platos en el catálogo.'
            }</p>`;
        }
        renderizarDetalle();
    }

    async function cargarProductos() {
        const respuesta = await solicitar('/api/productos?incluirInactivos=true');
        if (!Array.isArray(respuesta)) throw new Error('El catálogo no tiene un formato válido');
        productos = respuesta;
        if (productoSeleccionadoId && !productos.some(producto => String(producto.id) === String(productoSeleccionadoId))) {
            productoSeleccionadoId = null;
        }
        renderizarProductos();
    }

    async function cargarCategorias() {
        const respuesta = await solicitar('/api/categorias');
        if (!Array.isArray(respuesta)) throw new Error('La lista de categorías no tiene un formato válido');
        categorias = respuesta;
        selectorCategorias.replaceChildren(new Option('Selecciona una categoría', ''));
        categorias.forEach(categoria => selectorCategorias.add(new Option(categoria.nombre, categoria.id)));
        renderizarCategorias();
    }

    async function cambiarDisponibilidad(id, activo, control) {
        control.disabled = true;
        try {
            await solicitar(`/api/productos/${id}/disponibilidad?activo=${activo}`, { method: 'PATCH' });
            await cargarProductos();
            const producto = productoPorId(id);
            aviso(`${producto?.nombre || 'Plato'} ${activo ? 'activado' : 'pausado'} en el menú del mesero.`);
        } catch (error) {
            control.checked = !activo;
            console.error('No se pudo actualizar la disponibilidad del plato:', error);
            mostrarError(errorCatalogo, error.message || 'No se pudo actualizar la disponibilidad.');
        } finally {
            control.disabled = false;
        }
    }

    async function guardarImagen(id, archivo) {
        const datos = new FormData();
        datos.append('imagen', archivo);
        await solicitar(`/api/productos/${id}/imagen`, { method: 'POST', body: datos });
    }

    async function subirImagenSeleccionada() {
        if (!productoSeleccionadoId || !archivoImagenSeleccionado) return;
        const id = productoSeleccionadoId;
        botonGuardarImagen.disabled = true;
        ocultarError(errorImagenProducto);
        try {
            await guardarImagen(id, archivoImagenSeleccionado);
            archivoImagenSeleccionado = null;
            entradaImagenProducto.value = '';
            await cargarProductos();
            aviso(`${productoPorId(id)?.nombre || 'El plato'} ya muestra su imagen en el menú del mesero.`);
        } catch (error) {
            console.error('No se pudo guardar la imagen del plato:', error);
            mostrarError(errorImagenProducto, error.message || 'No se pudo guardar la imagen.');
        } finally {
            botonGuardarImagen.disabled = !archivoImagenSeleccionado;
        }
    }

    function abrirModalProducto() {
        ocultarError(errorProducto);
        modalProducto.classList.remove('hidden');
        modalProducto.classList.add('flex');
        formularioProducto.querySelector('[name="nombre"]').focus();
    }

    function cerrarModalProducto() {
        modalProducto.classList.remove('flex');
        modalProducto.classList.add('hidden');
        ocultarError(errorProducto);
    }

    async function guardarProducto(evento) {
        evento.preventDefault();
        ocultarError(errorProducto);
        const datos = new FormData(formularioProducto);
        const payload = {
            nombre: String(datos.get('nombre') || '').trim(),
            precio: Number(datos.get('precio')),
            categoriaId: Number(datos.get('categoriaId')),
            estacionKds: String(datos.get('estacionKds') || '').trim() || null
        };
        botonGuardarProducto.disabled = true;
        botonGuardarProducto.textContent = 'Guardando…';
        try {
            const producto = await solicitar('/api/productos', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            busquedaProductos.value = '';
            categoriaSeleccionada = 'all';
            filtroInactivos.checked = false;
            incluirInactivos = false;
            productoSeleccionadoId = producto.id;
            await cargarProductos();
            formularioProducto.reset();
            cerrarModalProducto();
            document.querySelector('[aria-label="Imagen del plato"]')
                .scrollIntoView({ behavior: 'smooth', block: 'center' });
            aviso(`${producto.nombre} quedó publicado. Puedes elegir su imagen desde el detalle.`);
        } catch (error) {
            console.error('No se pudo guardar el nuevo plato:', error);
            mostrarError(errorProducto, error.message || 'No se pudo guardar el plato.');
        } finally {
            botonGuardarProducto.disabled = false;
            botonGuardarProducto.textContent = 'Guardar y publicar';
        }
    }

    busquedaProductos.addEventListener('input', renderizarProductos);
    filtroCategorias.addEventListener('click', evento => {
        const boton = evento.target.closest('[data-category-filter]');
        if (!boton) return;
        categoriaSeleccionada = boton.dataset.categoryFilter;
        renderizarCategorias();
        renderizarProductos();
    });
    filtroInactivos.addEventListener('change', async () => {
        incluirInactivos = filtroInactivos.checked;
        ocultarError(errorCatalogo);
        renderizarProductos();
    });
    entradaImagenProducto.addEventListener('change', () => {
        const archivo = entradaImagenProducto.files?.[0] || null;
        if (archivo && !['image/jpeg', 'image/png'].includes(archivo.type)) {
            entradaImagenProducto.value = '';
            archivoImagenSeleccionado = null;
            mostrarError(errorImagenProducto, 'Selecciona una imagen JPG o PNG.');
        } else if (archivo && archivo.size > 5 * 1024 * 1024) {
            entradaImagenProducto.value = '';
            archivoImagenSeleccionado = null;
            mostrarError(errorImagenProducto, 'La imagen no debe superar 5 MB.');
        } else {
            archivoImagenSeleccionado = archivo;
            ocultarError(errorImagenProducto);
        }
        renderizarDetalle();
    });
    botonGuardarImagen.addEventListener('click', subirImagenSeleccionada);
    listaProductos.addEventListener('click', evento => {
        if (evento.target.closest('[data-availability-control]')) return;
        const tarjeta = evento.target.closest('[data-product-id]');
        if (!tarjeta) return;
        productoSeleccionadoId = Number(tarjeta.dataset.productId);
        renderizarProductos();
    });
    listaProductos.addEventListener('change', evento => {
        const control = evento.target.closest('[data-product-availability]');
        if (!control) return;
        cambiarDisponibilidad(Number(control.dataset.productAvailability), control.checked, control);
    });
    document.getElementById('btn-new-product').addEventListener('click', abrirModalProducto);
    document.getElementById('btn-close-product-modal').addEventListener('click', cerrarModalProducto);
    document.getElementById('btn-cancel-product').addEventListener('click', cerrarModalProducto);
    modalProducto.addEventListener('click', evento => {
        if (evento.target === modalProducto) cerrarModalProducto();
    });
    formularioProducto.addEventListener('submit', guardarProducto);
    document.addEventListener('keydown', evento => {
        if (evento.key === 'Escape' && !modalProducto.classList.contains('hidden')) cerrarModalProducto();
    });

    Promise.all([
        cargarCategorias().catch(error => {
            console.error('No se pudieron cargar las categorías:', error);
            mostrarError(errorCatalogo, error.message || 'No se pudieron cargar las categorías.');
        }),
        cargarProductos().catch(error => {
            console.error('No se pudo cargar el catálogo:', error);
            mostrarError(errorCatalogo, error.message || 'No se pudo cargar el catálogo.');
            listaProductos.innerHTML = '<p class="rounded-xl border border-secondary/40 bg-secondary-container/30 p-4 text-sm text-secondary">No se pudo cargar el catálogo. Inténtalo de nuevo.</p>';
        })
    ]);
})();
