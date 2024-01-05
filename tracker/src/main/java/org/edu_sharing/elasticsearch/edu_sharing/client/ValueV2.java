package org.edu_sharing.elasticsearch.edu_sharing.client;


import lombok.Data;

import java.util.List;


@Data
public class ValueV2{
	private String id,caption,description,parent,url;
	private List<String> alternativeIds;
}