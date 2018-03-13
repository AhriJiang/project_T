package com.juntao.project_T;

import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class test {
	public static void main(String[] args) throws IOException {
		
		String text="{\"aaa\":\"111\",\"bbb\": [\"ccc\", \"3\"]}";
		ObjectMapper mapper = new ObjectMapper(); 
		
		JsonNode rootNode = mapper.readTree(text); 
		
		Iterator<String> keys = rootNode.fieldNames(); 
		while(keys.hasNext()){    
            String fieldName = keys.next();    
            System.out.println("fieldName=" +fieldName+ " nodePath=" + rootNode.findPath(fieldName));  
            System.out.println("fieldDepth="+rootNode.findParents(fieldName).size());;
        }
		
		//JsonNode ----> JSON  
        System.out.println(mapper.writeValueAsString(rootNode)); 
	}
}
