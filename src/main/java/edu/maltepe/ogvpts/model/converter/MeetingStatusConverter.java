package edu.maltepe.ogvpts.model.converter;

import edu.maltepe.ogvpts.model.enums.MeetingStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MeetingStatusConverter extends LowercaseEnumConverter<MeetingStatus> {

    public MeetingStatusConverter() {
        super(MeetingStatus.class);
    }
}
