(() => {
  const board = document.getElementById('kds-orders');
  if (!board) return;

  const configurarNavegacion = () => {
    const navegacion = document.querySelector('nav[data-active-classes]');
    if (!navegacion) return;

    const clasesActivas = navegacion.dataset.activeClasses.split(/\s+/).filter(Boolean);
    const clasesInactivas = [
      'text-on-surface-variant',
      'hover:bg-surface-container-high',
      'hover:text-on-surface',
      'transition-colors'
    ];
    const operaciones = Array.from(navegacion.querySelectorAll('a[data-path]'));

    const seleccionarOperacion = seleccionada => {
      operaciones.forEach(operacion => {
        const activa = operacion === seleccionada;
        operacion.classList.toggle('bg-primary-container', activa);
        operacion.classList.toggle('text-on-primary-container', activa);
        operacion.classList.toggle('font-headline-sm', activa);
        operacion.classList.toggle('font-bold', activa);
        operacion.classList.toggle('shadow-md', activa);
        clasesInactivas.forEach(clase => operacion.classList.toggle(clase, !activa));
        if (activa) {
          operacion.setAttribute('aria-current', 'page');
        } else {
          operacion.removeAttribute('aria-current');
        }
      });
    };

    operaciones.forEach(operacion => {
      if (operacion.dataset.path === '16-cola-de-cocina-general-kds') {
        operacion.href = 'kds.html';
      } else if (operacion.dataset.path === '19-dashboard-de-rendimiento-de-cocina') {
        operacion.href = 'cocina-dashboard.html';
      }
      operacion.addEventListener('click', event => {
        seleccionarOperacion(operacion);
        if (operacion.getAttribute('href') === '#') event.preventDefault();
      });
    });

    const paginaActual = window.location.pathname.split('/').pop() || 'kds.html';
    const operacionActual = operaciones.find(operacion =>
      operacion.getAttribute('href') === paginaActual
    ) || operaciones[0];
    if (operacionActual) seleccionarOperacion(operacionActual);
  };

  const setBoardMessage = (message, isError = false) => {
    const notice = document.createElement('p');
    notice.className = `col-span-full rounded-xl p-6 text-center ${isError
      ? 'bg-secondary-container text-secondary-fixed'
      : 'bg-surface-container-low text-on-surface-variant'}`;
    notice.setAttribute('role', isError ? 'alert' : 'status');
    notice.textContent = message;
    board.replaceChildren(notice);
  };

  const createLabel = (text, className) => {
    const element = document.createElement('span');
    element.className = className;
    element.textContent = text;
    return element;
  };

  const formatearMinutos = segundos => `${Math.floor(Math.max(0, segundos) / 60)}m`;
  const formatearTiempoCocina = segundos => {
    const tiempo = Math.max(0, Math.floor(segundos));
    const minutos = Math.floor(tiempo / 60);
    const segundosRestantes = String(tiempo % 60).padStart(2, '0');
    return `${minutos}m ${segundosRestantes}s`;
  };
  const claveInicioCocina = pedidoId => `tabloncitos.kds.inicio-cocina.${pedidoId}`;

  const cerrarSesion = async () => {
    try {
      const response = await fetch('/api/auth/logout', { method: 'POST' });
      if (!response.ok) throw new Error(`Error HTTP ${response.status}`);
      localStorage.removeItem('tabloncitos.usuario');
      sessionStorage.removeItem('tabloncitos.usuario');
      window.location.replace('login.html');
    } catch (error) {
      console.error('No se pudo cerrar la sesión de cocina:', error);
      alert(`No se pudo cerrar la sesión: ${error.message}`);
    }
  };

  const configurarPanelSesion = usuario => {
    const etiquetas = document.querySelectorAll('header .flex.flex-col.text-right span');
    if (etiquetas?.length >= 3) {
      etiquetas[0].textContent = usuario.nombre || 'Cuenta de cocina';
      etiquetas[1].textContent = `COCINA · ${usuario.rol || 'COCINERO'}`;
      etiquetas[2].textContent = usuario.codigo ? `#${usuario.codigo}` : '';
    }

    const panel = document.querySelector('[data-path="reportar-incidencia-cocina"]')?.parentElement;
    if (!panel) return;

    const red = document.createElement('div');
    red.className = 'flex items-center justify-between rounded-lg bg-surface-container-lowest p-space-sm';
    const descripcionRed = document.createElement('div');
    descripcionRed.className = 'flex items-center gap-space-sm';
    const iconoRed = document.createElement('span');
    iconoRed.className = 'material-symbols-outlined flex h-7 w-7 items-center justify-center rounded-full bg-secondary-container text-secondary';
    iconoRed.textContent = 'wifi';
    const textosRed = document.createElement('div');
    textosRed.className = 'flex flex-col';
    textosRed.append(
      createLabel('RED SENATI', 'text-[10px] font-bold uppercase text-on-surface-variant'),
      createLabel(navigator.onLine ? 'Conectado' : 'Sin conexión', 'font-mono text-xs text-on-surface')
    );
    const indicador = document.createElement('span');
    indicador.className = `h-2.5 w-2.5 rounded-full ${navigator.onLine ? 'bg-primary-container' : 'bg-secondary'}`;
    descripcionRed.append(iconoRed, textosRed);
    red.append(descripcionRed, indicador);

    const logout = document.createElement('button');
    logout.type = 'button';
    logout.className = 'flex min-h-[44px] items-center justify-center gap-2 rounded-lg bg-surface-container-high px-4 py-2.5 text-xs font-bold uppercase text-on-surface-variant transition-colors hover:bg-surface-container-highest hover:text-on-surface';
    logout.append(
      createLabel('lock', 'material-symbols-outlined text-[18px]'),
      createLabel('Cerrar sesión', '')
    );
    logout.addEventListener('click', cerrarSesion);
    panel.replaceChildren(red, logout);
  };

  const crearTarjetaPedido = pedido => {
    const tarjeta = document.createElement('article');
    tarjeta.className = 'flex flex-col overflow-hidden rounded-xl bg-surface-container-low shadow-lg';
    const barra = document.createElement('div');
    barra.className = `h-2 w-full ${pedido.estadoPedido === 'LISTO'
      ? 'bg-tertiary-container'
      : pedido.estadoPedido === 'EN_PREPARACION' ? 'bg-primary-container' : 'bg-secondary-container'}`;

    const cabecera = document.createElement('div');
    cabecera.className = 'flex flex-col gap-1 bg-surface-container p-space-md';
    const filaCabecera = document.createElement('div');
    filaCabecera.className = 'flex items-center justify-between gap-space-sm';
    const mesa = pedido.mesa?.id ? `MESA #${pedido.mesa.id}` : String(pedido.tipoService || 'PEDIDO').toUpperCase();
    const identificadores = document.createElement('div');
    identificadores.className = 'flex items-center gap-space-xs';
    identificadores.append(
      createLabel(mesa, 'font-headline-md text-headline-md font-bold text-on-surface'),
      createLabel(`#COM-${pedido.id}`, 'font-label-caps text-label-caps font-bold text-primary')
    );
    const estado = pedido.estadoPedido === 'EN_PREPARACION'
      ? 'EN PREPARACIÓN'
      : pedido.estadoPedido === 'LISTO' ? 'LISTO' : 'EN COLA';
    const etiquetaEstado = createLabel(estado, 'rounded bg-surface-bright px-2 py-1 font-label-caps text-label-caps font-bold text-on-surface');
    filaCabecera.append(identificadores, etiquetaEstado);

    const detalleCabecera = document.createElement('div');
    detalleCabecera.className = 'mt-1 flex items-center justify-between gap-2 text-sm text-on-surface-variant';
    const mesero = createLabel(pedido.mozo ? `Mesero: ${pedido.mozo}` : 'Comanda nueva', 'truncate');
    const tiempoEnCola = createLabel(
      `HACE ${formatearMinutos(pedido.segundosEnCola ?? 0)}`,
      'shrink-0 font-mono font-bold text-primary'
    );
    tiempoEnCola.dataset.elapsedSeconds = String(pedido.segundosEnCola ?? 0);
    tiempoEnCola.dataset.updatedAt = String(Date.now());
    detalleCabecera.append(mesero, tiempoEnCola);
    cabecera.append(filaCabecera, detalleCabecera);
    if (pedido.estadoPedido === 'EN_PREPARACION' || pedido.estadoPedido === 'LISTO') {
      const ahora = Date.now();
      const segundosServidor = Number(pedido.segundosPreparacion ?? 0);
      let inicioPreparacion = Number(localStorage.getItem(claveInicioCocina(pedido.id)));
      if (!Number.isFinite(inicioPreparacion) || inicioPreparacion <= 0) {
        inicioPreparacion = ahora - segundosServidor * 1000;
        if (pedido.estadoPedido === 'EN_PREPARACION') {
          localStorage.setItem(claveInicioCocina(pedido.id), String(inicioPreparacion));
        }
      } else if (pedido.estadoPedido === 'LISTO') {
        localStorage.removeItem(claveInicioCocina(pedido.id));
      }
      const segundosGuardados = pedido.estadoPedido === 'EN_PREPARACION'
        ? Math.max(segundosServidor, Math.floor((ahora - inicioPreparacion) / 1000))
        : segundosServidor;
      const tiempoCoccion = createLabel(
        `EN COCINA ${formatearTiempoCocina(segundosGuardados)}`,
        'mt-1 inline-flex w-fit items-center gap-1 rounded bg-primary-container/20 px-2 py-1 font-mono text-xs font-bold text-primary'
      );
      if (pedido.estadoPedido === 'EN_PREPARACION') {
        tiempoCoccion.dataset.cookingSeconds = String(segundosGuardados);
        tiempoCoccion.dataset.updatedAt = String(ahora);
      }
      cabecera.append(tiempoCoccion);
    }

    const lista = document.createElement('div');
    lista.className = 'flex flex-1 flex-col gap-space-sm p-space-md';
    if (!Array.isArray(pedido.detalles) || pedido.detalles.length === 0) {
      lista.append(createLabel('Esta comanda no tiene platos.', 'text-sm text-on-surface-variant'));
    } else {
      pedido.detalles.forEach(detalle => {
        const fila = document.createElement('div');
        fila.className = 'flex items-start gap-space-sm rounded-lg bg-surface-container p-space-sm';
        fila.append(
          createLabel(`${detalle.cantidad}×`, 'shrink-0 font-headline-md text-headline-md font-bold text-primary'),
          createLabel(detalle.productoNombre || 'Producto', 'flex-1 font-headline-sm text-headline-sm font-bold text-on-surface')
        );
        if (detalle.observacion) {
          fila.append(createLabel(detalle.observacion, 'text-sm text-secondary'));
        }
        lista.append(fila);
      });
    }

    const acciones = document.createElement('div');
    acciones.className = 'bg-surface-container-lowest p-space-md';
    if (pedido.estadoPedido === 'EN_COLA' || pedido.estadoPedido === 'EN_PREPARACION') {
      const boton = document.createElement('button');
      boton.type = 'button';
      boton.className = 'min-h-[48px] w-full rounded-xl bg-primary-container px-4 py-2 font-bold text-on-primary-container transition-colors hover:brightness-110 disabled:opacity-60';
      boton.textContent = pedido.estadoPedido === 'EN_COLA' ? 'INICIAR PREPARACIÓN' : 'MARCAR COMO LISTO';
      boton.addEventListener('click', () => cambiarEstado(pedido.id, boton));
      acciones.append(boton);
    } else {
      acciones.append(createLabel('LISTO PARA RETIRAR', 'block text-center font-label-caps text-label-caps font-bold text-primary'));
    }

    tarjeta.append(barra, cabecera, lista, acciones);
    return tarjeta;
  };

  let actualizando = false;
  const cargarPedidos = async () => {
    if (actualizando) return;
    actualizando = true;
    try {
      const response = await fetch('/api/pedidos/cocina', { cache: 'no-store' });
      if (response.status === 401 || response.status === 403) {
        window.location.replace('login.html');
        return;
      }
      if (!response.ok) throw new Error(`Error HTTP ${response.status}`);
      const pedidos = await response.json();
      if (!Array.isArray(pedidos)) throw new Error('El servidor devolvió una cola de cocina no válida');
      const etiquetaCola = Array.from(document.querySelectorAll('header span'))
        .find(elemento => elemento.textContent.includes('EN COLA KDS:'));
      const contadorCola = etiquetaCola?.parentElement?.querySelector('span:last-child');
      if (contadorCola) contadorCola.textContent = String(pedidos.length);
      if (pedidos.length === 0) {
        setBoardMessage('No hay comandas pendientes de cocina.');
      } else {
        board.replaceChildren(...pedidos.map(crearTarjetaPedido));
      }
    } catch (error) {
      console.error('No se pudieron cargar las comandas de cocina:', error);
      setBoardMessage(`No se pudieron cargar las comandas: ${error.message}`, true);
    } finally {
      actualizando = false;
    }
  };

  const cambiarEstado = async (pedidoId, boton) => {
    const siguienteEstado = boton.textContent === 'INICIAR PREPARACIÓN' ? 'EN_PREPARACION' : 'LISTO';
    boton.disabled = true;
    try {
      const response = await fetch(`/api/pedidos/${pedidoId}/estado-cocina?nuevoEstado=${siguienteEstado}`, {
        method: 'PUT'
      });
      if (response.status === 404) throw new Error('No se encontró la comanda');
      const data = await response.json();
      if (!response.ok) throw new Error(data.mensaje || `Error HTTP ${response.status}`);
      if (siguienteEstado === 'EN_PREPARACION') {
        localStorage.setItem(claveInicioCocina(pedidoId), String(Date.now()));
      } else {
        localStorage.removeItem(claveInicioCocina(pedidoId));
      }
      await cargarPedidos();
    } catch (error) {
      console.error('No se pudo actualizar el estado de la comanda:', error);
      alert(`No se pudo actualizar la comanda: ${error.message}`);
      boton.disabled = false;
    }
  };

  window.setInterval(() => {
    board.querySelectorAll('[data-elapsed-seconds]').forEach(elemento => {
      const segundos = Number(elemento.dataset.elapsedSeconds);
      if (Number.isFinite(segundos)) {
        elemento.textContent = `HACE ${formatearMinutos(segundos + Math.floor((Date.now() - Number(elemento.dataset.updatedAt)) / 1000))}`;
      }
    });
    board.querySelectorAll('[data-cooking-seconds]').forEach(elemento => {
      const segundos = Number(elemento.dataset.cookingSeconds);
      if (Number.isFinite(segundos)) {
        const actualizadoEn = Number(elemento.dataset.updatedAt);
        const transcurrido = Number.isFinite(actualizadoEn)
          ? Math.floor((Date.now() - actualizadoEn) / 1000)
          : 0;
        elemento.textContent = `EN COCINA ${formatearTiempoCocina(segundos + transcurrido)}`;
      }
    });
  }, 1000);

  const iniciarKds = async () => {
    setBoardMessage('Conectando con la cola de cocina...');
    try {
      const response = await fetch('/api/auth/sesion', { cache: 'no-store' });
      if (!response.ok) {
        window.location.replace('login.html');
        return;
      }
      const usuario = await response.json();
      if (String(usuario.rol || '').toUpperCase() !== 'COCINERO') {
        window.location.replace('login.html');
        return;
      }
      configurarPanelSesion(usuario);
      await cargarPedidos();
      window.setInterval(cargarPedidos, 5000);
    } catch (error) {
      console.error('No se pudo verificar la cuenta de cocina:', error);
      setBoardMessage(`No se pudo verificar la cuenta: ${error.message}`, true);
    }
  };

  configurarNavegacion();
  iniciarKds();
})();
