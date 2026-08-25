package com.marcptr.cine.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import com.marcptr.cine.model.enums.ErrorCode;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MessageResolver {

    private final MessageSource messageSource;

    public String resolveMessage(ErrorCode code) {
        return messageSource.getMessage(
                "error." + code.name(),
                null,
                LocaleContextHolder.getLocale());
    }
}