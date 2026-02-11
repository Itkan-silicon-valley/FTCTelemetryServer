package org.firstinspires.ftc.teamcode.telelib;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

/**
 * Telemetry wrapper that mirrors SDK telemetry into the schema-driven telemetry bus.
 *
 * This lets existing code keep using telemetry.addData()/update() while also
 * publishing those fields to the TelemetryBus.
 */
public class TelelibTelemetry implements Telemetry, AutoCloseable {
    private static final String DEFAULT_NUMBER_FORMAT = "%.3f";
    private final Telemetry delegate;
    private final TelemetryBus bus;
    private final Map<String, ValueProvider> captured = new LinkedHashMap<>();
    private boolean busStarted;

    public TelelibTelemetry(Telemetry delegate, TelemetryBus bus) {
        this.delegate = delegate;
        this.bus = bus;
    }

    @Override
    public Item addData(String caption, String format, Object... args) {
        capture(caption, () -> String.format(Locale.US, format, args));
        captureNumeric(caption, format, args);
        return delegate.addData(caption, format, args);
    }

    @Override
    public Item addData(String caption, Object value) {
        capture(caption, () -> value == null ? "" : String.valueOf(value));
        captureNumeric(caption, value);
        return delegate.addData(caption, value);
    }

    @Override
    public <T> Item addData(String caption, Func<T> func) {
        capture(caption, () -> {
            T value = func.value();
            return value == null ? "" : String.valueOf(value);
        });
        captureNumeric(caption, func);
        return delegate.addData(caption, func);
    }

    @Override
    public <T> Item addData(String caption, String format, Func<T> func) {
        capture(caption, () -> {
            T value = func.value();
            return String.format(Locale.US, format, value);
        });
        captureNumeric(caption, format, func);
        return delegate.addData(caption, format, func);
    }

    @Override
    public boolean removeItem(Item item) {
        return delegate.removeItem(item);
    }

    @Override
    public void clear() {
        captured.clear();
        delegate.clear();
    }

    @Override
    public void clearAll() {
        captured.clear();
        delegate.clearAll();
    }

    @Override
    public Object addAction(Runnable action) {
        return delegate.addAction(action);
    }

    @Override
    public boolean removeAction(Object token) {
        return delegate.removeAction(token);
    }

    @Override
    public void speak(String text) {
        delegate.speak(text);
    }

    @Override
    public void speak(String text, String languageCode, String countryCode) {
        delegate.speak(text, languageCode, countryCode);
    }

    @Override
    public boolean update() {
        boolean result = delegate.update();
        mirrorToBus();
        if (delegate.isAutoClear()) {
            captured.clear();
        }
        return result;
    }

    @Override
    public Line addLine() {
        return delegate.addLine();
    }

    @Override
    public Line addLine(String lineCaption) {
        return delegate.addLine(lineCaption);
    }

    @Override
    public boolean removeLine(Line line) {
        return delegate.removeLine(line);
    }

    @Override
    public boolean isAutoClear() {
        return delegate.isAutoClear();
    }

    @Override
    public void setAutoClear(boolean autoClear) {
        delegate.setAutoClear(autoClear);
    }

    @Override
    public int getMsTransmissionInterval() {
        return delegate.getMsTransmissionInterval();
    }

    @Override
    public void setMsTransmissionInterval(int msTransmissionInterval) {
        delegate.setMsTransmissionInterval(msTransmissionInterval);
    }

    @Override
    public String getItemSeparator() {
        return delegate.getItemSeparator();
    }

    @Override
    public void setItemSeparator(String itemSeparator) {
        delegate.setItemSeparator(itemSeparator);
    }

    @Override
    public String getCaptionValueSeparator() {
        return delegate.getCaptionValueSeparator();
    }

    @Override
    public void setCaptionValueSeparator(String captionValueSeparator) {
        delegate.setCaptionValueSeparator(captionValueSeparator);
    }

    @Override
    public void setDisplayFormat(DisplayFormat displayFormat) {
        delegate.setDisplayFormat(displayFormat);
    }

    @Override
    public Log log() {
        return delegate.log();
    }

    @Override
    public void close() {
        if (bus != null) {
            bus.close();
        }
    }

    private void mirrorToBus() {
        if (bus == null || !bus.isEnabled()) {
            return;
        }
        if (!busStarted) {
            bus.start();
            busStarted = true;
        }
        bus.begin();
        for (Map.Entry<String, ValueProvider> entry : captured.entrySet()) {
            String value = TelemetrySchema.sanitizeCsv(entry.getValue().get());
            if (!tryPutNumber(entry.getKey(), value)) {
                bus.put(entry.getKey(), value);
            }
        }
        bus.publish();
    }

    private void capture(String caption, ValueProvider provider) {
        if (caption == null || caption.isEmpty()) {
            return;
        }
        captured.put(caption, provider);
    }

    private void captureNumeric(String caption, Object value) {
        if (caption == null || caption.isEmpty()) {
            return;
        }
        if (value instanceof Number) {
            captured.put(
                    caption, new NumericProvider(((Number) value).doubleValue(), DEFAULT_NUMBER_FORMAT));
        }
    }

    private void captureNumeric(String caption, String format, Object... args) {
        if (caption == null || caption.isEmpty()) {
            return;
        }
        if (args != null && args.length == 1 && args[0] instanceof Number) {
            captured.put(
                    caption, new NumericProvider(((Number) args[0]).doubleValue(), format));
        }
    }

    private <T> void captureNumeric(String caption, Func<T> func) {
        if (caption == null || caption.isEmpty()) {
            return;
        }
        T value = func.value();
        if (value instanceof Number) {
            captured.put(
                    caption, new NumericProvider(((Number) value).doubleValue(), DEFAULT_NUMBER_FORMAT));
        }
    }

    private <T> void captureNumeric(String caption, String format, Func<T> func) {
        if (caption == null || caption.isEmpty()) {
            return;
        }
        T value = func.value();
        if (value instanceof Number) {
            captured.put(caption, new NumericProvider(((Number) value).doubleValue(), format));
        }
    }

    private boolean tryPutNumber(String caption, String value) {
        ValueProvider provider = captured.get(caption);
        if (provider instanceof NumericProvider) {
            NumericProvider numeric = (NumericProvider) provider;
            bus.put(caption, numeric.value, numeric.format);
            return true;
        }
        return false;
    }

    private interface ValueProvider {
        String get();
    }

    private static final class NumericProvider implements ValueProvider {
        private final double value;
        private final String format;

        private NumericProvider(double value, String format) {
            this.value = value;
            this.format = format;
        }

        @Override
        public String get() {
            return String.format(Locale.US, format, value);
        }
    }
}
