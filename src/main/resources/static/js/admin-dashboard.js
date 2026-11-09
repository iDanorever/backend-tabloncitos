(() => {
    function buscarBoton(texto) {
        return [...document.querySelectorAll('main button')]
            .find(boton => boton.textContent.includes(texto));
    }

    function mostrarAviso(mensaje) {
        window.tabloncitosAdmin?.mostrarAviso(mensaje);
    }

    function configurarPestanas() {
        const pestanaRapida = buscarBoton('Vista Rápida del Día');
        const pestanaMetricas = buscarBoton('Métricas Avanzadas y Tendencias');
        const tituloMetricas = [...document.querySelectorAll('h2, h3')]
            .find(titulo => titulo.textContent.includes('Ritmo de Ventas Horarias vs. Meta'));

        if (!pestanaRapida || !pestanaMetricas || !tituloMetricas) return;

        const estilos = document.createElement('style');
        estilos.textContent = `
            .dashboard-tab-active {
                background-color: #ffc56c !important;
                color: #241700 !important;
                font-weight: 700 !important;
            }
            .dashboard-tab-active .material-symbols-outlined {
                color: #241700 !important;
            }
            #dashboard-trends {
                scroll-margin-top: 5rem;
            }
            @media print {
                aside, header {
                    display: none !important;
                }
                body, .pl-72 {
                    padding: 0 !important;
                }
                main {
                    padding-top: 0 !important;
                }
            }
        `;
        document.head.append(estilos);
        tituloMetricas.id = 'dashboard-trends';

        const activar = (seleccionada, otra, destino) => {
            seleccionada.classList.add('dashboard-tab-active');
            otra.classList.remove('dashboard-tab-active');
            seleccionada.setAttribute('aria-pressed', 'true');
            otra.setAttribute('aria-pressed', 'false');
            destino.scrollIntoView({ behavior: 'smooth', block: 'start' });
        };

        pestanaRapida.setAttribute('aria-pressed', 'true');
        pestanaRapida.addEventListener('click', () => {
            const inicio = document.querySelector('main');
            if (inicio) activar(pestanaRapida, pestanaMetricas, inicio);
        });
        pestanaMetricas.setAttribute('aria-pressed', 'false');
        pestanaMetricas.addEventListener('click', () => {
            activar(pestanaMetricas, pestanaRapida, tituloMetricas);
        });
    }

    function configurarAcciones() {
        const botonReporte = buscarBoton('Reporte PDF');
        const botonReposicion = buscarBoton('Gestionar Reposición');
        const botonNotificaciones = document.querySelector('header button');

        botonReporte?.addEventListener('click', () => window.print());
        botonReposicion?.addEventListener('click', () => {
            mostrarAviso('El módulo de inventario y reposición todavía no está disponible.');
        });
        botonNotificaciones?.addEventListener('click', () => {
            mostrarAviso('Alertas activas: carbón de algarrobo (3 sacos) y pisco quebranta (1 botella).');
        });
    }

    function configurarFiltros() {
        document.querySelectorAll('main select').forEach(filtro => {
            filtro.addEventListener('change', () => {
                const opcion = filtro.selectedOptions[0];
                mostrarAviso(`Filtro seleccionado: ${opcion?.textContent.trim() || filtro.value}. Los indicadores disponibles son referenciales.`);
            });
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        configurarPestanas();
        configurarAcciones();
        configurarFiltros();
    });
})();
