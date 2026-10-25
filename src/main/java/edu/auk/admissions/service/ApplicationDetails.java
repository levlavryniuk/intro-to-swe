package edu.auk.admissions.service;

/** Contact information and educational background typed into the application form. */
public record ApplicationDetails(
        String phone,
        String address,
        String lastSchool,
        int graduationYear,
        double gradeAverage) {
}
