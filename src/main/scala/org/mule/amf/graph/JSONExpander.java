package org.mule.amf.graph;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.system.PrefixMapNull;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.sparql.util.Context;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JSONExpander {

    private final String documentUrl;
    private final Model model;
    private final ObjectMapper jsonFactory = new ObjectMapper();

    public static String expand(String documentUrl, Model model) throws IOException {
        return new JSONExpander(documentUrl, model).canonicalJsonld();
    }

    private JSONExpander(String documentUrl, Model model) {
        this.documentUrl = documentUrl;
        this.model = model;
    }

    private String canonicalJsonld() throws IOException {
        StringWriter writer = new StringWriter();
        JsonLDWriter jsonld = new JsonLDWriter(RDFFormat.JSONLD_EXPAND_PRETTY);
        DatasetImpl ds = new DatasetImpl(model);
        jsonld.write(writer, ds.asDatasetGraph(), PrefixMapNull.empty, documentUrl, Context.emptyContext);
        return expandJson(documentUrl, writer.toString());
    }

    private String expandJson(String documentUrl, String json) throws IOException {
        JsonFactory factory = new JsonFactory();

        ObjectMapper mapper = new ObjectMapper(factory);
        JsonNode rootNode = mapper.readTree(json);
        HashMap<String, ObjectNode> acc = new HashMap<>();
        HashMap<String, JsonNode> expandedAcc = new HashMap<>();
        if (rootNode.isArray()) {
            for (final JsonNode objNode : rootNode) {
                String id = objNode.get("@id").asText();
                acc.put(id, (ObjectNode) objNode);
            }
            for (final JsonNode objNode : rootNode) {
                String id = objNode.get("@id").asText();
                JsonNode expandedNode = expandJsonNode(objNode, acc);
                expandedAcc.put(id, expandedNode);
            }
        }

        return jsonFactory.createArrayNode().add(expandedAcc.get(documentUrl)).toString();
    }

    private JsonNode expandJsonNode(JsonNode objNode, HashMap<String, ObjectNode> nodeMap) {
        ObjectNode mapped = (ObjectNode) objNode;
        Iterator<Map.Entry<String, JsonNode>> fields = objNode.fields();
        while(fields.hasNext()) {
            Map.Entry<String, JsonNode> pair = fields.next();

            if (pair.getValue().isArray()) {
                ArrayNode mappedValues = jsonFactory.createArrayNode();
                for (JsonNode value : pair.getValue()) {
                    if (value.isObject() && value.get("@id") != null) {
                        String id = value.get("@id").textValue();
                        if (nodeMap.keySet().contains(id)) {
                            mappedValues.add(nodeMap.get(id));
                        } else {
                            mappedValues.add(value);
                        }
                    } else {
                        mappedValues.add(value);
                    }
                }
                mapped.replace(pair.getKey(), mappedValues);
            }
        }
        return mapped;
    }
}
