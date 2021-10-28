package net.blancworks.figura.models.shaders;

import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A custom ResourceFactory for use with the Shader constructor.
 * It ignores namespaces, and instead gets its values from figura files.
 * This is specifically designed to work with the Shader constructor, as such it is incredibly janky and just overall bad.
 */

public class FiguraLocalShaderResourceFactory implements ResourceFactory {

    private Function<String, InputStream> inputStreamFunction;

    public FiguraLocalShaderResourceFactory(Path root) {
        inputStreamFunction = (str) -> {
            Path resourcePath = root.resolve(str);
            try {
                return Files.newInputStream(resourcePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };
    }

    public FiguraLocalShaderResourceFactory(ZipFile zip) {
        inputStreamFunction = (str) -> {
            ZipEntry entry = zip.getEntry(str);
            try {
                return zip.getInputStream(entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };
    }

    @Override
    public Resource getResource(Identifier identifier) throws IOException {
        return new Resource() {

            InputStream stream;

            @Override
            public Identifier getId() {
                return identifier;
            }

            @Override
            public InputStream getInputStream() {
                try {
                    //this.stream = DefaultResourcePack.this.open(ResourceType.CLIENT_RESOURCES, identifier);
                    String str = identifier.getPath();
                    //str currently looks something like:
                    //"shaders/core/UUIDUUID-UUID-UUID-UUID-UUIDUUIDUUID-IMPORTANT_INFO.json"
                    //where UUID is some random characters representing the player uuid
                    //We want to prune away the stuff at the front, leaving only IMPORTANT_INFO.fileExtension.
                    str = str.substring("shaders/core/UUIDUUID-UUID-UUID-UUID-UUIDUUIDUUID-".length());

                    stream = inputStreamFunction.apply(str);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return stream;
            }

            //Method is unused in shader constructor, so no need to do anything with it
            @Override
            public boolean hasMetadata() {
                return false;
            }

            //Method is unused in shader constructor, so no need to do anything with it
            @Nullable
            @Override
            public <T> T getMetadata(ResourceMetadataReader<T> resourceMetadataReader) {
                return null;
            }

            //This method's result only matters in the event of an exception occurring
            @Override
            public String getResourcePackName() {
                return "Custom Figura Render Layers";
            }

            @Override
            public void close() throws IOException {
                stream.close();
            }
        };
    }
}