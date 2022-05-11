package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class ValueV2{
	private String id,caption,description,parent,url;
	private List<String> alternativeIds;
	public ValueV2(){};


	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public void setUrl(String url) { this.url = url; }
	public String getUrl() { return url; }

	public void setAlternativeIds(List<String> alternativeIds) { this.alternativeIds = alternativeIds; }

	public List<String> getAlternativeIds() { return alternativeIds; }
}