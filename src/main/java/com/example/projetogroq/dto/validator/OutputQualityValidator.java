package com.example.projetogroq.dto.validator;

import com.example.projetogroq.dto.OutputQuality;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OutputQualityValidator implements ConstraintValidator<ValidOutputQuality, String> {
    @Override
    public void initialize(ValidOutputQuality constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(value == null){
            return true;
        }

        for(OutputQuality quality : OutputQuality.values()){
            if(quality.name().equals(value)){
                return true;
            }
        }

        return false;
    }
}
