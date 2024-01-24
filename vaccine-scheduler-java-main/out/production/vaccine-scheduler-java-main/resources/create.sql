CREATE TABLE Caregivers (
    Username varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Patients (
    pUsername varchar(255),
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (pUsername)
);

CREATE TABLE Availabilities (
    Time date,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Appointment (
     Time date,
     Appointment_ID INT UNIQUE,
     Username varchar(255) REFERENCES Caregivers,
     pUsername varchar(255) REFERENCES Patients,
     Name varchar(255) REFERENCES Vaccines,
     PRIMARY KEY (Time, Appointment_ID)
);

CREATE TABLE Vaccines (
    Name varchar(255),
    Doses int,
    PRIMARY KEY (Name)
);