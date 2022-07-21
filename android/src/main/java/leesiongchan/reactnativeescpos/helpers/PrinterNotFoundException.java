package leesiongchan.reactnativeescpos.helpers;

public class PrinterNotFoundException extends RuntimeException {
    public PrinterNotFoundException(String message) {
        super(message);
    }
}