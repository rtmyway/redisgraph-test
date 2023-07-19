package com.gantang.test.redisgraph;

public class Node {

    private String id;
    private String type;
    private String name;
    private String parenId;
    private String parenType;
    private String pathType;    

    public Node(String id, String type, String name, String parenId, String parenType, String pathType) {
        super();
        this.id = id;
        this.type = type;
        this.name = name;
        this.parenId = parenId;
        this.parenType = parenType;
        this.pathType = pathType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParenId() {
        return parenId;
    }

    public void setParenId(String parenId) {
        this.parenId = parenId;
    }

    public String getParenType() {
        return parenType;
    }

    public void setParenType(String parenType) {
        this.parenType = parenType;
    }

    public String getPathType() {
        return pathType;
    }

    public void setPathType(String pathType) {
        this.pathType = pathType;
    }

}
