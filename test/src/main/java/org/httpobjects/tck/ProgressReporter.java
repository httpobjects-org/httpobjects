package org.httpobjects.tck;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ProgressReporter {
    private final String label;
    private final BigInteger total;
    final BigInteger increment;

    public ProgressReporter(String label, BigInteger total, BigInteger increment) {
        this.label = label;
        this.total = total;
        this.increment = (increment == null) ? (total!=null ? total.divide(BigInteger.valueOf(100)) : BigInteger.valueOf(10000)) : increment;
    }

    public void progressMade(Long progress){
        this.progressMade(BigInteger.valueOf(progress));
    }
    public void progressMade(BigInteger progress){
        if(progress.mod(increment) == BigInteger.ZERO){
            final String percentage = (total == null) ? "" : new BigDecimal(progress).divide(new BigDecimal(total)).multiply(new BigDecimal(100)).setScale(2).toPlainString() + "%";
            System.out.println("[" + label + "] " + percentage + " " + progress + "/" + total + " (increments of " + increment + ")");
        }
    }
}
