package com.banknode.atm;

import com.banknode.atm.exception.AtmException;
import com.banknode.atm.model.Cajero;
import com.banknode.atm.model.Cliente;
import com.banknode.atm.repository.CajeroRepository;
import com.banknode.atm.repository.ClienteRepository;
import com.banknode.atm.service.CajeroService;
import com.banknode.atm.service.ClienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suite completa de pruebas unitarias y de integracion para BankNode-ATM.
 */
@SpringBootTest
@Transactional
class BanknodeAtmApplicationTests {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CajeroService cajeroService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CajeroRepository cajeroRepository;

    @Test
    @DisplayName("1. El contexto de Spring Boot debe cargar y poblar los 3 clientes y cajero inicial")
    void contextLoadsYDatosIniciales() {
        assertNotNull(clienteService);
        assertNotNull(cajeroService);

        assertTrue(clienteRepository.count() >= 3, "Deben existir al menos 3 clientes iniciales en H2");
        assertTrue(cajeroRepository.count() >= 1, "Debe existir al menos un cajero inicial en H2");

        Cliente c1 = clienteService.consultarPorDocumento("1001");
        assertEquals("Carlos Mendoza", c1.getNombre());
        assertEquals(new BigDecimal("1500000.00"), c1.consultarSaldo());
    }

    @Test
    @DisplayName("2. La entidad Cliente debe ejecutar correctamente consultarSaldo(), consignar() y retirar()")
    void testOperacionesEntidadCliente() {
        Cliente cliente = new Cliente("9999", "Prueba Unitaria", new BigDecimal("100000.00"), "0000");

        // Consultar Saldo
        assertEquals(new BigDecimal("100000.00"), cliente.consultarSaldo());

        // Consignar
        cliente.consignar(new BigDecimal("50000.00"));
        assertEquals(new BigDecimal("150000.00"), cliente.consultarSaldo());

        // Retirar válido
        cliente.retirar(new BigDecimal("70000.00"));
        assertEquals(new BigDecimal("80000.00"), cliente.consultarSaldo());

        // Retirar con saldo insuficiente debe lanzar AtmException
        assertThrows(AtmException.class, () -> cliente.retirar(new BigDecimal("90000.00")));
    }

    @Test
    @DisplayName("3. La clase Cajero debe gestionar inventario de billetes ($10k, $20k, $50k, $100k) y despachar correctamente")
    void testGestionInventarioCajero() {
        Cajero cajero = new Cajero(10, 10, 10, 10);
        // Total: 10*10k (100k) + 10*20k (200k) + 10*50k (500k) + 10*100k (1.000k) = $1.800.000 COP
        assertEquals(new BigDecimal("1800000"), cajero.calcularTotalDisponible());

        // Verificar disponibilidad para $170.000 (1 de 100k, 1 de 50k, 1 de 20k)
        Map<Integer, Integer> distribucion = cajero.verificarDisponibilidad(new BigDecimal("170000"));
        assertEquals(1, distribucion.get(100000));
        assertEquals(1, distribucion.get(50000));
        assertEquals(1, distribucion.get(20000));
        assertEquals(0, distribucion.get(10000));

        // Despachar billetes y validar descuento de inventario
        String reporte = cajero.despacharBilletes(new BigDecimal("170000"));
        assertNotNull(reporte);
        assertEquals(9, cajero.getBilletes100k());
        assertEquals(9, cajero.getBilletes50k());
        assertEquals(9, cajero.getBilletes20k());
        assertEquals(10, cajero.getBilletes10k());
        assertEquals(new BigDecimal("1630000"), cajero.calcularTotalDisponible());

        // Retiro que no sea múltiplo de 10.000 debe fallar
        assertThrows(AtmException.class, () -> cajero.verificarDisponibilidad(new BigDecimal("25500")));
    }

    @Test
    @DisplayName("4. Autenticación exitosa con documento y PIN de 4 dígitos")
    void testAutenticacionExitosa() {
        Cliente autenticado = clienteService.autenticar("1001", "1234");
        assertNotNull(autenticado);
        assertEquals(0, autenticado.getIntentosFallidos());
        assertNull(autenticado.getBloqueadoHasta());
    }

    @Test
    @DisplayName("5. Bloqueo de cuenta por 10 minutos tras 3 intentos fallidos consecutivos")
    void testBloqueoPorTresIntentosFallidos() {
        Cliente cliente = new Cliente("8888", "Usuario Seguridad", new BigDecimal("500000.00"), "1111");
        clienteRepository.save(cliente);

        // Intento 1 fallido
        assertThrows(AtmException.class, () -> clienteService.autenticar("8888", "9999"));
        Cliente intento1 = clienteService.consultarPorDocumento("8888");
        assertEquals(1, intento1.getIntentosFallidos());

        // Intento 2 fallido
        assertThrows(AtmException.class, () -> clienteService.autenticar("8888", "9999"));
        Cliente intento2 = clienteService.consultarPorDocumento("8888");
        assertEquals(2, intento2.getIntentosFallidos());

        // Intento 3 fallido -> Provoca bloqueo de 10 minutos
        AtmException ex3 = assertThrows(AtmException.class, () -> clienteService.autenticar("8888", "9999"));
        assertTrue(ex3.getMessage().contains("bloqueada durante 10 minutos"));

        Cliente intento3 = clienteService.consultarPorDocumento("8888");
        assertEquals(3, intento3.getIntentosFallidos());
        assertNotNull(intento3.getBloqueadoHasta());
        assertTrue(intento3.isBloqueado());

        // Intento 4 aún con PIN correcto debe ser rechazado por estar bloqueado
        AtmException exBloqueado = assertThrows(AtmException.class, () -> clienteService.autenticar("8888", "1111"));
        assertTrue(exBloqueado.getMessage().contains("Cuenta bloqueada por seguridad"));
    }

    @Test
    @DisplayName("6. Transferencia entre cuentas debita origen y acredita destino atómicamente")
    void testTransferenciaEntreCuentas() {
        Cliente origen = clienteService.consultarPorDocumento("1001");
        Cliente destino = clienteService.consultarPorDocumento("1002");

        BigDecimal saldoOrigenInicial = origen.consultarSaldo();
        BigDecimal saldoDestinoInicial = destino.consultarSaldo();
        BigDecimal montoTransferir = new BigDecimal("200000.00");

        clienteService.transferir("1001", "1002", montoTransferir);

        Cliente origenActualizado = clienteService.consultarPorDocumento("1001");
        Cliente destinoActualizado = clienteService.consultarPorDocumento("1002");

        assertEquals(0, saldoOrigenInicial.subtract(montoTransferir).compareTo(origenActualizado.consultarSaldo()));
        assertEquals(0, saldoDestinoInicial.add(montoTransferir).compareTo(destinoActualizado.consultarSaldo()));
    }

    @Test
    @DisplayName("7. Retiro procesado a través de CajeroService descuenta saldo de cliente y billetes del cajero")
    void testRetiroCompletoCajeroService() {
        Cliente cliente = clienteService.consultarPorDocumento("1003");
        BigDecimal saldoInicial = cliente.consultarSaldo();
        BigDecimal totalCajeroInicial = cajeroService.consultarTotalDisponible();

        BigDecimal montoRetiro = new BigDecimal("180000.00"); // 1 de 100k + 1 de 50k + 1 de 20k + 1 de 10k
        String desglose = cajeroService.procesarRetiro("1003", montoRetiro);

        assertNotNull(desglose);
        assertTrue(desglose.contains("Billetes entregados"));

        Cliente clienteDespues = clienteService.consultarPorDocumento("1003");
        BigDecimal totalCajeroDespues = cajeroService.consultarTotalDisponible();

        assertEquals(0, saldoInicial.subtract(montoRetiro).compareTo(clienteDespues.consultarSaldo()));
        assertEquals(0, totalCajeroInicial.subtract(montoRetiro).compareTo(totalCajeroDespues));
    }
}
