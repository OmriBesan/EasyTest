package com.easydine.app.utils;

import com.easydine.app.data.model.Booking; //this use our real class called booking.

public class BookingLogic {


    public boolean canReserve(int currentAvailableSeats, int requestedSeats) {
        if (requestedSeats <= 0) return false;
        return requestedSeats <= currentAvailableSeats;
    }


    public int processCancellation(int currentAvailableSeats, Booking bookingToCancel) {
        // Safety check: If booking is null, don't change anything
        if (bookingToCancel == null) {
            return currentAvailableSeats;
        }

        // Logic: Get the party size from the booking and ADD it back to available seats
        int seatsRestored = bookingToCancel.getPartySize();

        return currentAvailableSeats + seatsRestored;
    }


}