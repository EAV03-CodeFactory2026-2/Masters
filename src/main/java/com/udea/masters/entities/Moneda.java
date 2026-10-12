package com.udea.masters.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "moneda")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Moneda {

    @Id
    @Column(name = "moneda_codigo_iso", length = 3)
    private String monedaCodigoIso;

    @Column(name = "nombre_moneda", nullable = false, length = 50)
    private String nombreMoneda;
}
