package org.edu_sharing.elasticsearch.edu_sharing.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Setter;

import java.util.List;

@Setter
public class SortV2 {
	@Data
	public static class SortV2Default{
		private String sortBy;
		private boolean sortAscending;

	}
	private String id;
	private SortV2Default defaultValue;
	private List<SortColumnV2> columns;

	public SortV2(){}


	@JsonProperty
	public String getId() {
		return id;
	}

	@JsonProperty
	public List<SortColumnV2> getColumns() {
		return columns;
	}

	@JsonProperty("default")
	public SortV2Default getDefaultValue() {
		return defaultValue;
	}

}

