package com.mycompany.oop.service;

import com.mycompany.oop.model.Employee;
import com.mycompany.oop.model.RegularEmployee;

import java.lang.reflect.Field;

final class ServiceTestSupport {

    private ServiceTestSupport() {
    }

    static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to prepare service test dependency.", e);
        }
    }

    static Employee employee(int employeeId, String firstName, String lastName, String role) {
        return new RegularEmployee(
                employeeId,
                firstName,
                lastName,
                "Team Member",
                "Active",
                25000,
                1500,
                100,
                "",
                "",
                role);
    }
}
