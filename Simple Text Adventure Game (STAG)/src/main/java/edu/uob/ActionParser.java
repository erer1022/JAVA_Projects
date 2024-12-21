package edu.uob;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ActionParser {

    private HashMap<String, HashSet<GameAction>> actions = new HashMap<>();

    public HashMap<String, HashSet<GameAction>> parseActionsFromFile(File actionsFile) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(actionsFile);

            NodeList actionList = document.getElementsByTagName("action");
            for (int i = 0; i < actionList.getLength(); i++) {
                org.w3c.dom.Node actionNode = actionList.item(i);
                if (actionNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    GameAction action = parseAction((Element) actionNode);
                    addToDataStructure(action);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse actions file", e);
        }
        return actions;
    }

    private GameAction parseAction(Element actionElement) {
        List<String> triggers = getChildrenTextContent(actionElement, "triggers", "keyphrase");
        List<String> subjects = getChildrenTextContent(actionElement, "subjects", "entity");
        List<String> consumed = getChildrenTextContent(actionElement, "consumed", "entity");
        List<String> produced = getChildrenTextContent(actionElement, "produced", "entity");
        String narration = getSingleChildTextContent(actionElement, "narration");

        return new GameAction(triggers, subjects, consumed, produced, narration);
    }

    private List<String> getChildrenTextContent(Element parentElement, String parentTagName, String childTagName) {
        List<String> contents = new ArrayList<>();
        NodeList parentNodes = parentElement.getElementsByTagName(parentTagName);
        if (parentNodes.getLength() > 0) {
            NodeList childNodes = ((Element) parentNodes.item(0)).getElementsByTagName(childTagName);
            for (int i = 0; i < childNodes.getLength(); i++) {
                org.w3c.dom.Node node = childNodes.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    contents.add(node.getTextContent().trim());
                }
            }
        }
        return contents;
    }

    private String getSingleChildTextContent(Element parentElement, String childTagName) {
        NodeList nodes = parentElement.getElementsByTagName(childTagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    private void addToDataStructure(GameAction action) {
        for (String trigger : action.getTriggers()) {
            actions.computeIfAbsent(trigger, k -> new HashSet<>()).add(action);
        }
    }
}
