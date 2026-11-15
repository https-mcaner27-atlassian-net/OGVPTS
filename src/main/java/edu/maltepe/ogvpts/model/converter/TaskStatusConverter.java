package edu.maltepe.ogvpts.model.converter;

import edu.maltepe.ogvpts.model.enums.TaskStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TaskStatusConverter extends LowercaseEnumConverter<TaskStatus> {

    public TaskStatusConverter() {
        super(TaskStatus.class);
    }
}
