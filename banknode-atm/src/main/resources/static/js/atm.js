// Script para gestion de inactividad de 180 segundos y comodidades de interfaz ATM
document.addEventListener('DOMContentLoaded', () => {

    // 1. Temporizador de Inactividad de 180 segundos para Pantalla de Operaciones
    const timerElement = document.getElementById('timerSeconds');
    const timerContainer = document.getElementById('timerContainer');

    if (timerElement) {
        let segundosRestantes = 180;
        let intervalId = null;

        const actualizarVistaTimer = () => {
            const minutos = Math.floor(segundosRestantes / 60);
            const seg = segundosRestantes % 60;
            timerElement.textContent = `${minutos.toString().padStart(2, '0')}:${seg.toString().padStart(2, '0')}`;

            if (segundosRestantes <= 30) {
                timerContainer.classList.add('timer-warning');
            } else {
                timerContainer.classList.remove('timer-warning');
            }
        };

        const reiniciarInactividad = () => {
            segundosRestantes = 180;
            actualizarVistaTimer();
        };

        // Escuchar actividad del usuario para reiniciar el contador de 180s de inactividad
        const eventos = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
        eventos.forEach(evento => {
            document.addEventListener(evento, reiniciarInactividad, { passive: true });
        });

        // Intervalo de cuenta regresiva
        intervalId = setInterval(() => {
            segundosRestantes--;
            actualizarVistaTimer();

            if (segundosRestantes <= 0) {
                clearInterval(intervalId);
                window.location.href = '/logout?motivo=inactividad';
            }
        }, 1000);

        actualizarVistaTimer();
    }

    // 2. Botones de Retiro Rápido
    const botonesRapidos = document.querySelectorAll('.btn-monto-rapido');
    const inputMontoRetiro = document.getElementById('montoRetiro');

    if (botonesRapidos && inputMontoRetiro) {
        botonesRapidos.forEach(btn => {
            btn.addEventListener('click', () => {
                const valor = btn.getAttribute('data-monto');
                inputMontoRetiro.value = valor;
            });
        });
    }

    // 3. Auto-ocultar alertas informativas tras 6 segundos
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) {
                bsAlert.close();
            }
        }, 6000);
    });
});
