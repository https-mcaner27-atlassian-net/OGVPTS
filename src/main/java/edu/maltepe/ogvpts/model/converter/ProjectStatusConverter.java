package edu.maltepe.ogvpts.model.converter;

import edu.maltepe.ogvpts.model.enums.ProjectStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ProjectStatusConverter extends LowercaseEnumConverter<ProjectStatus> {

    public ProjectStatusConverter() {
        super(ProjectStatus.class);
    }
}
