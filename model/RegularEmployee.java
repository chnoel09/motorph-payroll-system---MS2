package com.mycompany.oop.model;

/**
 * Concrete employee type used by payroll processing.
 */
public class RegularEmployee extends Employee {

    public RegularEmployee(
            int employeeId, 
            String firstName, 
            String lastName, 
            String position, 
            String employmentStatus, 
            double basicSalary, 
            double allowance, 
            double hourlyRate,
            String username,
            String password,
            String role

    ) {
        
        super(
                employeeId, 
                firstName, 
                lastName, 
                position, 
                employmentStatus, 
                basicSalary, 
                allowance, 
                hourlyRate,
                username,
                password,
                role
        
        );

                

    }    
   
    @Override
    public double computeGrossSalary() {
        return getBasicSalary() + getAllowance();
    } 
    
    @Override
    public double computeDeductions() {
        return 0; // handled by PayrollProcessor
    }
}
