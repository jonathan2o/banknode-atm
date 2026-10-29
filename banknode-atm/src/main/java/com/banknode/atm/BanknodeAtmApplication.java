package com.banknode.atm;

import com.banknode.atm.model.Cliente;
import com.banknode.atm.repository.ClienteRepository;
import com.banknode.atm.service.CajeroService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;
import java.util.List;

/**
 * Clase principal de inicio de la aplicacion BankNode-ATM y carga de datos iniciales.
 */
@SpringBootApplication
public class BanknodeAtmApplication {

    public static void main(String[] args) {
        SpringApplication.run(BanknodeAtmApplication.class, args);
    }

    /**
     * Inicializador de datos de prueba para poblar al menos 3 clientes y el inventario del cajero en H2.
     */
    @Bean
    public CommandLineRunner initDatabase(ClienteRepository clienteRepository, CajeroService cajeroService) {
        return args -> {
            // Inicializar Cajero si no existe con billetes suficientes
            // 40 billetes de 100k ($4.000.000) + 50 de 50k ($2.500.000) + 50 de 20k ($1.000.000) + 100 de 10k ($1.000.000) = $8.500.000 COP
            cajeroService.inicializarCajeroSiNoExiste(100, 50, 50, 40);

            // Inicializar al menos 3 clientes de prueba
            if (clienteRepository.count() == 0) {
                Cliente c1 = new Cliente("1001", "Carlos Mendoza", new BigDecimal("1500000.00"), "1234");
                Cliente c2 = new Cliente("1002", "Laura Gómez", new BigDecimal("850000.00"), "4321");
                Cliente c3 = new Cliente("1003", "Andrés Restrepo", new BigDecimal("2300000.00"), "9876");

                clienteRepository.saveAll(List.of(c1, c2, c3));
            }
        };
    }
}
