package com.alessandro.falldetector.utils;


public class Contact {

    private String name;
    private String number;

    public Contact(String name, String number){

        this.name=name;
        this.number=number;
    }

    public String getNumber() {
        return number;
    }
    public void setNumber(String number) {
        this.number = number;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        Contact other = (Contact) obj;

        if(this.getName().equals(other.getName()) && this.getNumber().equals(other.getNumber()))
            return true;
        else
            return false;
    }

}