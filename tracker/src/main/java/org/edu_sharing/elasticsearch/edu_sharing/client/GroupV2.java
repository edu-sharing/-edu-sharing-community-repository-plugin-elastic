package org.edu_sharing.elasticsearch.edu_sharing.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroupV2 {
		private String id;
		private List<String> views;
}

