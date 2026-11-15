package edu.maltepe.ogvpts.model.converter;

import edu.maltepe.ogvpts.model.enums.FeedbackDecision;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FeedbackDecisionConverter extends LowercaseEnumConverter<FeedbackDecision> {

    public FeedbackDecisionConverter() {
        super(FeedbackDecision.class);
    }
}
