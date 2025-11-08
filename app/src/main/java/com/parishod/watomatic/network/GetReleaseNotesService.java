package com.parishod.watomagic.network;

import com.parishod.watomagic.model.GithubReleaseNotes;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface GetReleaseNotesService {
    @GET("/repos//releases")
    Call<List<GithubReleaseNotes>> getReleaseNotes();
}
