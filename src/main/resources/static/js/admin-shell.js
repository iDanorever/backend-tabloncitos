(() => {
    const rutasModulos = new Map([
        ['Gestión de Empleados', 'admin-empleados.html'],
        ['Dashboard Gerencial', 'admin-dashboard.html'],
        ['Precios & Promociones', 'admin-precios.html'],
        ['Catálogo & Menú', 'admin-menu.html'],
        ['Salón & Mesas', 'mesas.html']
    ]);

    let aviso;
    let avisoTimer;

    function mostrarAviso(mensaje, esError = false) {
        if (!aviso) {
            aviso = document.createElement('div');
            aviso.setAttribute('role', esError ? 'alert' : 'status');
            aviso.setAttribute('aria-live', 'polite');
            aviso.className = 'admin-shell-notice';
            document.body.append(aviso);
        }

        aviso.textContent = mensaje;
        aviso.classList.toggle('admin-shell-notice-error', esError);
        aviso.classList.add('admin-shell-notice-visible');
        window.clearTimeout(avisoTimer);
        avisoTimer = window.setTimeout(() => {
            aviso.classList.remove('admin-shell-notice-visible');
        }, 4500);
    }

    function instalarEstilos() {
        const estilos = document.createElement('style');
        estilos.textContent = `
            aside {
                background-color: #181615 !important;
                border-right: 1px solid #2a2420 !important;
                box-shadow: 0 1px 8px rgba(0, 0, 0, 0.4) !important;
                color: #e6e2df;
            }
            aside .bg-surface-container {
                background-color: #1f1c1a !important;
            }
            aside .bg-surface-container-high {
                background-color: #25211e !important;
            }
            aside .text-on-surface {
                color: #e6e2df !important;
            }
            aside .text-on-surface-variant {
                color: #d6c4ac !important;
            }
            aside .text-outline {
                color: #8d7d6c !important;
            }
            aside .text-headline-sm {
                font-size: 18px;
                line-height: 24px;
                font-weight: 600;
            }
            aside .text-body-md {
                font-size: 14px;
                line-height: 20px;
                font-weight: 400;
            }
            aside .text-body-sm {
                font-size: 12px;
                line-height: 16px;
                font-weight: 400;
            }
            aside .text-label-caps {
                font-size: 11px;
                line-height: 14px;
                letter-spacing: 0.06em;
                font-weight: 700;
            }
            aside .text-label-numeric-sm {
                font-size: 12px;
                line-height: 14px;
                font-weight: 500;
            }
            header .text-label-caps {
                font-size: 11px;
                line-height: 14px;
                letter-spacing: 0.06em;
                font-weight: 700;
            }
            aside .rounded-lg {
                border-radius: 0.25rem !important;
            }
            aside .rounded-xl {
                border-radius: 0.5rem !important;
            }
            aside .rounded-full {
                border-radius: 9999px !important;
            }
            aside > div:first-child > div:nth-child(2) > div {
                background-color: #25211e !important;
            }
            aside > div:first-child > div:first-child > div:first-child {
                background: #c84b31 !important;
                background-image: none !important;
                border-radius: 0.25rem !important;
                color: #fff !important;
            }
            aside .text-primary {
                color: #ffc56c !important;
            }
            aside .bg-primary {
                background-color: #ffc56c !important;
            }
            aside .text-on-primary {
                color: #442c00 !important;
            }
            aside nav a[aria-current="page"] {
                background-color: rgba(200, 75, 49, 0.2) !important;
                border: 1px solid rgba(200, 75, 49, 0.4) !important;
                color: #ffc56c !important;
                font-weight: 600 !important;
            }
            aside nav a[aria-current="page"] .material-symbols-outlined {
                color: #c84b31 !important;
            }
            .admin-shell-notice {
                position: fixed;
                right: 1rem;
                bottom: 1rem;
                z-index: 100;
                max-width: min(28rem, calc(100vw - 2rem));
                padding: 0.75rem 1rem;
                border: 1px solid #c84b31;
                border-radius: 0.5rem;
                background: #1f1c1a;
                color: #f1ece7;
                box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
                opacity: 0;
                pointer-events: none;
                transform: translateY(0.5rem);
                transition: opacity 160ms ease, transform 160ms ease;
            }
            .admin-shell-notice-visible {
                opacity: 1;
                transform: translateY(0);
            }
            .admin-shell-notice-error {
                border-color: #e53935;
            }
        `;
        document.head.append(estilos);
    }

    function configurarNavegacion() {
        const paginaActual = window.location.pathname.split('/').pop() || 'admin-empleados.html';

        document.querySelectorAll('aside nav a').forEach(enlace => {
            const etiqueta = [...enlace.querySelectorAll('span')]
                .filter(span => !span.classList.contains('material-symbols-outlined'))
                .map(span => span.textContent.trim())
                .filter(texto => texto && !/^\d+$/.test(texto))
                .join(' ');
            const destino = rutasModulos.get(etiqueta);

            if (!destino) {
                enlace.setAttribute('aria-disabled', 'true');
                enlace.title = 'Este módulo todavía no está disponible';
                enlace.addEventListener('click', event => {
                    event.preventDefault();
                    mostrarAviso(`${etiqueta}: este módulo todavía no está disponible.`);
                });
                return;
            }

            enlace.href = destino;
            if (destino === paginaActual) {
                enlace.setAttribute('aria-current', 'page');
            } else {
                enlace.removeAttribute('aria-current');
            }
        });
    }

    function actualizarPerfil(usuario) {
        const perfil = document.querySelector('aside > div:last-child > div:first-child');
        if (!perfil) return;

        const datos = perfil.querySelector('div.flex-col')?.querySelectorAll('span');
        const nombre = datos?.[0];
        const rol = datos?.[1];
        const codigo = perfil.querySelector(':scope > span:last-child');
        if (nombre && usuario.nombre) nombre.textContent = usuario.nombre;
        if (rol && usuario.rol) {
            const roles = { ADMIN: 'Gerente General', COCINERO: 'Cocinero', MOZO: 'Mozo', MESERO: 'Mesero', CAJERO: 'Cajero' };
            rol.textContent = roles[usuario.rol.toUpperCase()] || usuario.rol;
        }
        if (codigo && usuario.codigo) codigo.textContent = usuario.codigo;
    }

    async function verificarSesion() {
        try {
            const respuesta = await fetch('/api/auth/sesion', { cache: 'no-store' });
            if (respuesta.status === 401) {
                window.location.assign('login.html');
                return;
            }
            if (!respuesta.ok) throw new Error(`Error HTTP ${respuesta.status}`);

            const usuario = await respuesta.json();
            actualizarPerfil(usuario);
            localStorage.setItem('tabloncitos.usuario', JSON.stringify(usuario));
        } catch (error) {
            console.error('No se pudo verificar la sesión administrativa:', error);
            mostrarAviso('No se pudo verificar la cuenta iniciada. Comprueba tu conexión e inténtalo de nuevo.', true);
        }
    }

    function configurarSalida() {
        const controlesSalir = [...document.querySelectorAll('aside a, aside button')]
            .filter(control => control.textContent.trim().toLocaleLowerCase().includes('salir'));

        controlesSalir.forEach(control => {
            control.addEventListener('click', async event => {
                event.preventDefault();
                control.setAttribute('aria-disabled', 'true');
                control.classList.add('pointer-events-none', 'opacity-60');

                try {
                    const respuesta = await fetch('/api/auth/logout', { method: 'POST' });
                    if (!respuesta.ok) throw new Error(`Error HTTP ${respuesta.status}`);
                    localStorage.removeItem('tabloncitos.usuario');
                    sessionStorage.removeItem('tabloncitos.usuario');
                    window.location.assign('login.html');
                } catch (error) {
                    console.error('No se pudo cerrar la sesión administrativa:', error);
                    control.removeAttribute('aria-disabled');
                    control.classList.remove('pointer-events-none', 'opacity-60');
                    mostrarAviso('No se pudo cerrar la sesión. Inténtalo nuevamente.', true);
                }
            });
        });
    }

    window.tabloncitosAdmin = { mostrarAviso };

    document.addEventListener('DOMContentLoaded', () => {
        instalarEstilos();
        configurarNavegacion();
        configurarSalida();
        verificarSesion();
    });
})();
