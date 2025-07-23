package com.gaumji.tests.flightreservations.model;

public record FlightReservationTestData(String firstname,
                                        String lastname,
                                        String email,
                                        String password,
                                        String street,
                                        String city,
                                        String zip,
                                        String passengersCount,
                                        String expectedPrice) {


}
