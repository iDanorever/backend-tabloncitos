package com.tabloncitos.backend.config;

import com.tabloncitos.backend.service.ProductoImagenStorage;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;

@Configuration
public class ProductoImagenWebConfig implements WebMvcConfigurer {

    private final ProductoImagenStorage productoImagenStorage;

    public ProductoImagenWebConfig(ProductoImagenStorage productoImagenStorage) {
        this.productoImagenStorage = productoImagenStorage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        URI directorio = productoImagenStorage.getDirectorio().toUri();
        String ubicacion = directorio.toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registry.addResourceHandler("/images/productos/**").addResourceLocations(ubicacion);
    }
}
