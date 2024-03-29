package com.springBootProject.formula1.service;

import com.springBootProject.formula1.domain.Driver;
import com.springBootProject.formula1.repository.DriverRepository;
import com.springBootProject.formula1.response.ResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    /**
 * Retrieve a list of drivers based on the given search criteria.
 *
 * @param driver (optional) the driver name or a substring of it to search for
 * @param team (optional) the team name or a substring of it to search for
 * @param position (optional) the driver position to search for
 * @param year (optional) the year to search for
 * @return a ResponseEntity containing the list of matching drivers, or an error message if no results are found
 */
    @Autowired
    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public ResponseEntity<Object> get(Optional<String> driver,
                                      Optional<String> team,
                                      Optional<Integer> position,
                                      Optional<Integer> year) {
        // Start with an empty specification
        Specification<Driver> spec = Specification.where(null);

        if (driver.isPresent()) { // Driver.name contains 'driver'?
            String pattern = "%" + driver.get() + "%";
            spec = spec.and((root, query, cb) -> cb.like(root.get("name"), pattern));
        }

        if (team.isPresent()) { // Driver.team contains 'team'?
            String pattern = "%" + team.get() + "%";
            spec = spec.and((root, query, cb) -> cb.like(root.get("team"), pattern));
        }

        if (position.isPresent()) { // Driver.position equals 'position'?
            spec = spec.and((root, query, cb) -> cb.equal(root.get("position"), position.get()));
        }

        if (year.isPresent()) { // Driver.year equals 'year'?
            spec = spec.and((root, query, cb) -> cb.equal(root.get("year"), year.get()));
        }

        // Execute the query with the generated specification and sort the results by ID
        List<Driver> data = driverRepository.findAll(spec, Sort.by("id"));

        if (data.isEmpty()) {
            return ResponseHandler.generateResponse("No results found", HttpStatus.NOT_FOUND, data);
        } else {
            return ResponseHandler.generateResponse("Success", HttpStatus.OK, data);
        }
    }

    public ResponseEntity<Object> add(List<Driver> drivers) {
        List<Driver> existingDrivers = new ArrayList<>(Collections.emptyList());
        // Check input data for existing drivers
        for (Driver driver : drivers) {
            if (driverRepository.existsByNameAndYearAndTeam(
                    driver.getName(),
                    driver.getYear(),
                    driver.getTeam())) {
                existingDrivers.add(driver);
            }
        }
        if(existingDrivers.isEmpty()) {
            // Input validation OK, add drivers
            for (Driver driver : drivers) {
                driverRepository.save(driver);
            }
            return ResponseHandler.generateResponse("Drivers created successfully", HttpStatus.CREATED, drivers);
        } else {
            return ResponseHandler.generateResponse("Existing drivers detected", HttpStatus.CONFLICT, existingDrivers);
        }
    }

    @Transactional
    public ResponseEntity<Object> update(Long id,
                                         Driver newDriver) {
        Driver driver = driverRepository.findById(id).orElse(new Driver());
        if(driver.getId() == null) {
            return ResponseHandler.generateResponse("Driver with ID " + id + " does not exist", HttpStatus.NOT_FOUND);
        } else {
            if (driverRepository.existsByNameAndYearAndTeam(
                    newDriver.getName(),
                    newDriver.getYear(),
                    newDriver.getTeam())) {
                return ResponseHandler.generateResponse("Driver already exists", HttpStatus.CONFLICT);
            }
            driver.setYear(newDriver.getYear());
            driver.setName(newDriver.getName());
            driver.setTeam(newDriver.getTeam());
            return ResponseHandler.generateResponse("Driver updated successfully", HttpStatus.OK, driver);
        }
    }

    public ResponseEntity<Object> delete(Long id) {
        Optional<Driver> driver = driverRepository.findById(id);
        if(driver.isEmpty()) {
            return ResponseHandler.generateResponse("Driver with ID " + id + " does not exist", HttpStatus.NOT_FOUND);
        } else {
            driverRepository.deleteById(id);
            return ResponseHandler.generateResponse("Driver deleted successfully", HttpStatus.OK, driver);
        }
    }
}
