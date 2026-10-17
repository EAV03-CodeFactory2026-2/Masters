package com.udea.masters.controllers.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegocioCreateRequest {

    @NotBlank(message = "El nombre del negocio no puede estar vacío.")
    @Size(max = 250, message = "El nombre no puede exceder los 250 caracteres.")
    private String nombreNegocio;

    @NotBlank(message = "La dirección no puede estar vacía.")
    @Size(max = 250, message = "La dirección no puede exceder los 250 caracteres.")
    private String direccionNegocio;

    @NotBlank(message = "El teléfono no puede estar vacío.")
    @Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$", message = "El formato del teléfono es inválido.")
    private String telefono;

    @NotBlank(message = "La identificación fiscal no puede estar vacía.")
    @Size(max = 20, message = "La identificación fiscal no puede exceder los 20 caracteres.")
    private String identificacionFiscal;

    @NotBlank(message = "Debe especificar un código de moneda válido.")
    @Size(min = 3, max = 3, message = "El código ISO de la moneda debe ser de 3 letras.")
    private String monedaCodigoIso;
}
