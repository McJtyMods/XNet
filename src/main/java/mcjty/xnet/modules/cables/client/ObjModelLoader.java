package mcjty.xnet.modules.cables.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.model.loading.v1.UnbakedModelDeserializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.joml.Vector3f;

/** Native model geometry for the existing antenna OBJ/MTL resources, including item models. */
public final class ObjModelLoader {
    private record Vertex(Vector3f position, Vector2f uv) { }
    private record Face(List<Vertex> vertices, String texture) { }

    public static void register() {
        UnbakedModelDeserializer.register(Identifier.parse("xnet:obj"), (json, context) -> {
            JsonObject vanilla = json.deepCopy();
            vanilla.remove("fabric:type");
            UnbakedModel delegate = context.deserialize(vanilla, UnbakedModel.class);
            var id = Identifier.parse(json.get("model").getAsString());
            var faces = read(id, json.has("flip_v") && json.get("flip_v").getAsBoolean());
            return new UnbakedModel() {
                @Override public Boolean ambientOcclusion() { return delegate.ambientOcclusion(); }
                @Override public GuiLight guiLight() { return delegate.guiLight(); }
                @Override public ItemTransforms transforms() { return delegate.transforms(); }
                @Override public TextureSlots.Data textureSlots() { return delegate.textureSlots(); }
                @Override public Identifier parent() { return delegate.parent(); }
                @Override public UnbakedGeometry geometry() {
                    return (slots, baker, state, debug) -> {
                        var result = new QuadCollection.Builder();
                        for (var face : faces) {
                            var material = baker.materials().resolveSlot(slots, face.texture(), debug);
                            var sprite = material.sprite();
                            var positions = new Vector3f[4];
                            var uv = new long[4];
                            for (int i = 0; i < 4; i++) {
                                var vertex = face.vertices().get(i);
                                positions[i] = new Vector3f(vertex.position()).sub(.5f, .5f, .5f);
                                state.transformation().getMatrix().transformPosition(positions[i]);
                                positions[i].add(.5f, .5f, .5f);
                                uv[i] = UVPair.pack(sprite.getU(vertex.uv().x), sprite.getV(vertex.uv().y));
                            }
                            var normal = new Vector3f(positions[1]).sub(positions[0])
                                    .cross(new Vector3f(positions[2]).sub(positions[0])).normalize();
                            var info = BakedQuad.MaterialInfo.of(material, sprite.transparency(), -1, null, 0);
                            result.addUnculledFace(new BakedQuad(positions[0], positions[1], positions[2], positions[3],
                                    uv[0], uv[1], uv[2], uv[3], Direction.getApproximateNearest(normal.x, normal.y, normal.z), info));
                        }
                        return result.build();
                    };
                }
            };
        });
    }

    private static List<Face> read(Identifier id, boolean flipV) {
        var positions = new ArrayList<Vector3f>();
        var uv = new ArrayList<Vector2f>();
        var materials = new HashMap<String, String>();
        var faces = new ArrayList<Face>();
        String material = "";
        try (var reader = open(id)) {
            for (String line; (line = reader.readLine()) != null;) {
                String[] parts = line.strip().split("\\s+");
                switch (parts[0]) {
                    case "v" -> positions.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                    case "vt" -> uv.add(new Vector2f(Float.parseFloat(parts[1]), flipV ? 1 - Float.parseFloat(parts[2]) : Float.parseFloat(parts[2])));
                    case "mtllib" -> readMaterials(Identifier.fromNamespaceAndPath(id.getNamespace(),
                            id.getPath().substring(0, id.getPath().lastIndexOf('/') + 1) + parts[1]), materials);
                    case "usemtl" -> material = parts[1];
                    case "f" -> {
                        var vertices = new ArrayList<Vertex>();
                        for (int i = 1; i < parts.length; i++) {
                            var indices = parts[i].split("/");
                            var position = positions.get(index(indices[0], positions.size()));
                            var tex = indices.length > 1 && !indices[1].isEmpty() ? uv.get(index(indices[1], uv.size())) : new Vector2f();
                            vertices.add(new Vertex(position, tex));
                        }
                        String texture = materials.getOrDefault(material, "particle");
                        if (vertices.size() == 4) faces.add(new Face(List.copyOf(vertices), texture));
                        else for (int i = 1; i + 1 < vertices.size(); i++) faces.add(new Face(
                                List.of(vertices.get(0), vertices.get(i), vertices.get(i + 1), vertices.get(i + 1)), texture));
                    }
                    default -> { }
                }
            }
        } catch (IOException | RuntimeException exception) {
            throw new JsonParseException("Cannot load OBJ model " + id, exception);
        }
        return List.copyOf(faces);
    }

    private static int index(String text, int size) {
        int value = Integer.parseInt(text);
        return value < 0 ? size + value : value - 1;
    }

    private static void readMaterials(Identifier id, Map<String, String> materials) throws IOException {
        String material = "";
        try (var reader = open(id)) {
            for (String line; (line = reader.readLine()) != null;) {
                String[] parts = line.strip().split("\\s+");
                if (parts[0].equals("newmtl")) material = parts[1];
                else if (parts[0].equals("map_Kd")) materials.put(material, parts[1]);
            }
        }
    }

    private static BufferedReader open(Identifier id) throws IOException {
        return Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).openAsReader();
    }
}
