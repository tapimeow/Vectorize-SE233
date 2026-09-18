package se233.vectorize.adapter;

import se233.vectorize.exception.ConversionException;
import se233.vectorize.model.VectorizationSettings;

import java.nio.file.Path;
import java.util.function.BooleanSupplier;

/** Replaceable boundary between prepared raster data and real SVG tracing. */
public interface VectorEngine {
    String trace(Path preparedPng, VectorizationSettings settings, String ignoredColor,
                 BooleanSupplier cancelled) throws ConversionException;
}
