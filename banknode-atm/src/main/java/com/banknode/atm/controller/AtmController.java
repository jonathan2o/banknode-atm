package com.banknode.atm.controller;

import com.banknode.atm.exception.AtmException;
import com.banknode.atm.model.Cliente;
import com.banknode.atm.service.CajeroService;
import com.banknode.atm.service.ClienteService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Controlador principal para la navegacion y flujo operativo del cajero automatico BankNode-ATM.
 */
@Controller
public class AtmController {

    private final ClienteService clienteService;
    private final CajeroService cajeroService;

    public AtmController(ClienteService clienteService, CajeroService cajeroService) {
        this.clienteService = clienteService;
        this.cajeroService = cajeroService;
    }

    /**
     * Redirige la ruta raiz a la pantalla de disponibilidad o login.
     */
    @GetMapping("/")
    public String inicio() {
        return "redirect:/login";
    }

    /**
     * PANTALLA 1: Disponibilidad de billetes en el cajero automatico.
     */
    @GetMapping("/disponibilidad")
    public String pantallaDisponibilidad(Model model) {
        BigDecimal totalDisponible = cajeroService.consultarTotalDisponible();
        Map<String, Integer> inventario = cajeroService.consultarInventarioBilletes();

        model.addAttribute("totalDisponible", totalDisponible);
        model.addAttribute("inventario", inventario);

        return "disponibilidad";
    }

    /**
     * PANTALLA 2: Formulario de inicio de sesion con Documento y PIN de 4 digitos.
     */
    @GetMapping("/login")
    public String pantallaLogin(HttpSession session, Model model) {
        // Si ya cuenta con sesion activa, enviar directamente a operaciones
        if (session.getAttribute("clienteDoc") != null) {
            return "redirect:/operaciones";
        }
        return "login";
    }

    /**
     * Procesa la autenticacion del cliente.
     */
    @PostMapping("/login")
    public String procesarLogin(@RequestParam("documento") String documento,
                                @RequestParam("pin") String pin,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Cliente cliente = clienteService.autenticar(documento, pin);
            session.setAttribute("clienteDoc", cliente.getDocumento());
            session.setAttribute("clienteNombre", cliente.getNombre());
            redirectAttributes.addFlashAttribute("successMessage", "¡Bienvenido, " + cliente.getNombre() + "!");
            return "redirect:/operaciones";
        } catch (AtmException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    /**
     * PANTALLA 3: Panel de Operaciones (Consultar Saldo, Retirar, Transferir, Salir) con timer de 180s.
     */
    @GetMapping("/operaciones")
    public String pantallaOperaciones(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String clienteDoc = (String) session.getAttribute("clienteDoc");
        if (clienteDoc == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Debe iniciar sesión para realizar operaciones.");
            return "redirect:/login";
        }

        try {
            Cliente cliente = clienteService.consultarPorDocumento(clienteDoc);
            BigDecimal totalCajero = cajeroService.consultarTotalDisponible();

            model.addAttribute("cliente", cliente);
            model.addAttribute("saldo", cliente.consultarSaldo());
            model.addAttribute("totalCajero", totalCajero);
            model.addAttribute("tiempoLimiteSegundos", 180);

            return "operaciones";
        } catch (AtmException e) {
            session.invalidate();
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    /**
     * Procesa el retiro de dinero con validacion de fondos y despacho de billetes.
     */
    @PostMapping("/retirar")
    public String procesarRetiro(@RequestParam("monto") BigDecimal monto,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        String clienteDoc = (String) session.getAttribute("clienteDoc");
        if (clienteDoc == null) {
            return "redirect:/login";
        }

        try {
            String desgloseBilletes = cajeroService.procesarRetiro(clienteDoc, monto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Retiro realizado exitosamente por $" + monto.toPlainString() + " COP. " + desgloseBilletes);
        } catch (AtmException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/operaciones";
    }

    /**
     * Procesa la transferencia de dinero a otra cuenta destino.
     */
    @PostMapping("/transferir")
    public String procesarTransferencia(@RequestParam("docDestino") String docDestino,
                                        @RequestParam("monto") BigDecimal monto,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        String clienteDoc = (String) session.getAttribute("clienteDoc");
        if (clienteDoc == null) {
            return "redirect:/login";
        }

        try {
            clienteService.transferir(clienteDoc, docDestino, monto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Transferencia de $" + monto.toPlainString() + " COP realizada con éxito a la cuenta documento: " + docDestino);
        } catch (AtmException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/operaciones";
    }

    /**
     * Cierra la sesion del cliente y redirige a la pantalla de login.
     */
    @GetMapping("/logout")
    public String cerrarSesion(@RequestParam(value = "motivo", required = false) String motivo,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        session.invalidate();
        if ("inactividad".equalsIgnoreCase(motivo)) {
            redirectAttributes.addFlashAttribute("infoMessage", "Sesión cerrada automáticamente por inactividad (180 segundos).");
        } else {
            redirectAttributes.addFlashAttribute("infoMessage", "Operaciones finalizadas. Gracias por utilizar BankNode-ATM.");
        }
        return "redirect:/login";
    }
}
