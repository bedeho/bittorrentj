package org.bittorrentj.util;

/**
 * Created by bedeho on 10.09.2014.
 *
 * Represents the registration of an extension message.
 */
public class extensionRegistration {

    private String name;
    private int id;

    extensionRegistration(String name, int id){
        this.name = name;
        this.id = id;
    }

    public boolean isEnabled() {
        return getId() != 0;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof extensionRegistration)) return false;

        extensionRegistration that = (extensionRegistration) o;

        if (id != that.id) return false;
        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + id;
        return result;
    }
}