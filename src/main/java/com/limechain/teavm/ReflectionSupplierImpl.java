package com.limechain.teavm;

import com.limechain.teavm.annotation.Reflectable;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReflectionSupplierImpl implements ReflectionSupplier {
    @Override
    public Collection<String> getAccessibleFields(ReflectionContext context, String className) {
        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return Collections.emptyList();
        }
        Set<String> fields = new HashSet<>();
        if (cls.getAnnotations().get(Reflectable.class.getName()) != null) {
            List<String> descriptors = cls.getFields().stream()
                .map(FieldReader::getName)
                .toList();
            fields.addAll(descriptors);
        } else {
            for (FieldReader field : cls.getFields()) {
                if (field.getAnnotations().get(Reflectable.class.getName()) != null) {
                    fields.add(field.getName());
                }
            }
        }
        return fields;
    }

    @Override
    public Collection<MethodDescriptor> getAccessibleMethods(ReflectionContext context, String className) {
        ClassReader cls = context.getClassSource().get(className);
        if (cls == null) {
            return Collections.emptyList();
        }
        Set<MethodDescriptor> methods = new HashSet<>();
        if (cls.getAnnotations().get(Reflectable.class.getName()) != null) {
            List<MethodDescriptor> descriptors = cls.getMethods().stream()
                .map(MethodReader::getDescriptor)
                .toList();
            methods.addAll(descriptors);
        } else {
            for (MethodReader method : cls.getMethods()) {
                if (method.getAnnotations().get(Reflectable.class.getName()) != null) {
                    methods.add(method.getDescriptor());
                }
            }
        }
        return methods;
    }
}
