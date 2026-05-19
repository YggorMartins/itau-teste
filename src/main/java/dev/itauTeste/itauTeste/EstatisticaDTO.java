package dev.itauTeste.itauTeste;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EstatisticaDTO {
    private long count;
    private double sum;
    private double avg;
    private double min;
    private double max;
}