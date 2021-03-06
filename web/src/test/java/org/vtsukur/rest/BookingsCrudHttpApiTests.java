package org.vtsukur.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.vtsukur.rest.core.domain.Booking;
import org.vtsukur.rest.core.domain.BookingRepository;
import org.vtsukur.rest.core.domain.Hotel;
import org.vtsukur.rest.core.domain.Room;
import org.vtsukur.rest.etc.money.Currencies;
import org.vtsukur.rest.styles.crud.mvc.BookingSaveRequest;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.vtsukur.rest.MapBasedBookingRepresentationMatcher.isBooking;

/**
 * @author volodymyr.tsukur
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@Transactional
public class BookingsCrudHttpApiTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Fixture fixture;

    @Autowired
    private ObjectMapper jsonSerializer;

    @Autowired
    private BookingRepository bookingRepository;

    private Room referenceRoom;

    private Booking referenceBooking;

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        fixture.init();
        Hotel oneOfTheHotels = fixture.getNobilis();

        referenceRoom = oneOfTheHotels.getRooms().iterator().next();
        referenceBooking = new Booking(
                LocalDate.of(2015, 9, 1),
                LocalDate.of(2015, 9, 10),
                Money.of(500, Currencies.USD),
                referenceRoom,
                Booking.Status.CREATED
        );
    }

    @Test
    public void post() throws Exception {
        final String content = jsonSerializer.writeValueAsString(
                new BookingSaveRequest(
                        referenceBooking.getCheckIn(),
                        referenceBooking.getCheckOut(),
                        referenceBooking.getRoom().getId())
        );
        final ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders
                .post("/crud/bookings")
                .accept(MediaType.APPLICATION_JSON)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON));
        final Booking createdBooking = bookingRepository.findAll(new Sort(Sort.Direction.DESC, "id")).iterator().next();
        resultActions
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/crud/bookings/" + createdBooking.getId()))
                .andExpect(jsonPath("$", isBooking(createdBooking)));
    }

    @Test
    public void getOne() throws Exception {
        bookingRepository.save(referenceBooking);

        mockMvc.perform(MockMvcRequestBuilders
                .get("/crud/bookings/" + referenceBooking.getId())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isBooking(referenceBooking)));
    }

    @Test
    public void mergeOne() throws Exception {
        bookingRepository.save(referenceBooking);

        final String content = jsonSerializer.writeValueAsString(
                new BookingSaveRequest(
                        referenceBooking.getCheckIn().plusDays(10),
                        referenceBooking.getCheckOut().plusDays(10),
                        referenceBooking.getRoom().getId())
        );
        mockMvc.perform(MockMvcRequestBuilders
                .patch("/crud/bookings/" + referenceBooking.getId())
                .accept(MediaType.APPLICATION_JSON)
                .content(content)
                .contentType(RestMediaTypes.MERGE_PATCH_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isBooking(referenceBooking)));
    }

    @Test
    public void deleteOne() throws Exception {
        bookingRepository.save(referenceBooking);

        mockMvc.perform(MockMvcRequestBuilders
                .delete("/crud/bookings/" + referenceBooking.getId()))
                .andExpect(status().isNoContent());
    }

}
