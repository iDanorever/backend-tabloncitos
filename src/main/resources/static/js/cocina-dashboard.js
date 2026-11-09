(() => {
  const crearEtiqueta = (texto, clase) => {
    const etiqueta = document.createElement('span');
    etiqueta.className = clase;
    etiqueta.textContent = texto;
    return etiqueta;
  };

  const configurarNavegacion = () => {
    const navegacion = document.querySelector('nav[data-active-classes]');
    if (!navegacion) return;

    const operaciones = Array.from(navegacion.querySelectorAll('a[data-path]'));
    operaciones.forEach(operacion => {
      if (operacion.dataset.path === '16-cola-de-cocina-general-kds') {
        operacion.href = 'kds.html';
      } else if (operacion.dataset.path === '19-dashboard-de-rendimiento-de-cocina') {
        operacion.href = 'cocina-dashboard.html';
      }
    });

    const paginaActual = window.location.pathname.split('/').pop() || 'cocina-dashboard.html';
    const operacionActual = operaciones.find(operacion =>
      operacion.getAttribute('href') === paginaActual
    );
    if (operacionActual) {
      navegacion.dataset.activeClasses.split(/\s+/).filter(Boolean).forEach(clase => {
        operacionActual.classList.add(clase);
      });
      operacionActual.classList.remove(
        'text-on-surface-variant',
        'hover:bg-surface-container-high',
        'hover:text-on-surface',
        'transition-colors'
      );
      operacionActual.setAttribute('aria-current', 'page');
    }
  };

  const actualizarEstadoRed = () => {
    const estado = document.getElementById('kitchen-network-status');
    const indicador = document.getElementById('kitchen-network-indicator');
    if (!estado || !indicador) return;

    const conectado = navigator.onLine;
    estado.textContent = conectado ? 'Conectado' : 'Sin conexión';
    indicador.classList.toggle('bg-primary-container', conectado);
    indicador.classList.toggle('bg-secondary', !conectado);
  };

  const cerrarSesion = async () => {
    const boton = document.getElementById('kitchen-logout');
    if (boton) boton.disabled = true;
    try {
      const response = await fetch('/api/auth/logout', { method: 'POST' });
      if (!response.ok) throw new Error(`Error HTTP ${response.status}`);
      localStorage.removeItem('tabloncitos.usuario');
      sessionStorage.removeItem('tabloncitos.usuario');
      window.location.replace('login.html');
    } catch (error) {
      console.error('No se pudo cerrar la sesión de cocina:', error);
      alert(`No se pudo cerrar la sesión: ${error.message}`);
      if (boton) boton.disabled = false;
    }
  };

  const configurarSesion = async () => {
    const identidad = document.querySelector('header .flex.flex-col.text-right');
    if (!identidad) return;

    const etiquetas = identidad.querySelectorAll('span');
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
    if (etiquetas.length >= 3) {
      etiquetas[0].textContent = usuario.nombre || 'Cuenta de cocina';
      etiquetas[1].textContent = `COCINA · ${usuario.rol}`;
      etiquetas[2].textContent = usuario.codigo ? `#${usuario.codigo}` : '';
    }

    const panel = document.querySelector('aside > div:last-child');
    if (panel) {
      const red = document.createElement('div');
      red.className = 'flex items-center justify-between rounded-lg bg-surface-container-lowest p-space-sm';
      const detalleRed = document.createElement('div');
      detalleRed.className = 'flex items-center gap-space-sm';
      const icono = crearEtiqueta(
        'wifi',
        'material-symbols-outlined flex h-7 w-7 items-center justify-center rounded-full bg-secondary-container text-secondary'
      );
      const textoRed = document.createElement('div');
      textoRed.className = 'flex flex-col';
      textoRed.append(
        crearEtiqueta('RED SENATI', 'text-[10px] font-bold uppercase text-on-surface-variant'),
        crearEtiqueta('', 'font-mono text-xs text-on-surface')
      );
      textoRed.lastElementChild.id = 'kitchen-network-status';
      const indicador = crearEtiqueta(
        '',
        'h-2.5 w-2.5 rounded-full'
      );
      indicador.id = 'kitchen-network-indicator';
      detalleRed.append(icono, textoRed);
      red.append(detalleRed, indicador);

      const botonSalir = document.createElement('button');
      botonSalir.id = 'kitchen-logout';
      botonSalir.type = 'button';
      botonSalir.className = 'flex min-h-[44px] items-center justify-center gap-2 rounded-lg bg-surface-container-high px-4 py-2.5 text-xs font-bold uppercase text-on-surface-variant transition-colors hover:bg-surface-container-highest hover:text-on-surface disabled:opacity-60';
      botonSalir.append(
        crearEtiqueta('lock', 'material-symbols-outlined text-[18px]'),
        crearEtiqueta('Cerrar sesión', '')
      );
      botonSalir.addEventListener('click', cerrarSesion);
      panel.replaceChildren(red, botonSalir);
      actualizarEstadoRed();
      window.addEventListener('online', actualizarEstadoRed);
      window.addEventListener('offline', actualizarEstadoRed);
    }
  };

  configurarNavegacion();
  configurarSesion().catch(error => {
    console.error('No se pudo verificar la cuenta de cocina:', error);
    alert(`No se pudo verificar la cuenta de cocina: ${error.message}`);
  });
})();
