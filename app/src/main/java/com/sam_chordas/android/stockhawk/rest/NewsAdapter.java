package com.sam_chordas.android.stockhawk.rest;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.info.Article;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by Owen LaRosa on 9/11/16.
 */

public class NewsAdapter extends RecyclerView.Adapter {

    private ArrayList<Article> articles;

    public NewsAdapter() {
        articles = new ArrayList<Article>();
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_article, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Article article = articles.get(position);
        ViewHolder viewHolder = (ViewHolder) holder;
        viewHolder.titleTextView.setText(article.title);
        viewHolder.urlTextView.setText(article.link);
    }

    /**
     * Add an article to the adapter
     * @param article Article to be added
     */
    public void addArticle(Article article) {
        articles.add(article);
        notifyDataSetChanged();
    }

    /**
     * Add the collection of articles to the adapter
     * @param newArticles Articles to be added
     */
    public void addAllArticles(Collection<Article> newArticles) {
        articles.addAll(newArticles);
        notifyDataSetChanged();
    }

    /**
     * Remove article at the specified index
     * @param index Index of article to be removed
     */
    public void removeAtIndex(int index) {
        articles.remove(index);
        notifyDataSetChanged();
    }

    /**
     * Remove all articles from the adapter
     */
    public void clear() {
        articles.clear();
        notifyDataSetChanged();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView titleTextView;
        public final TextView urlTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.article_title_text_view);
            urlTextView = (TextView) itemView.findViewById(R.id.article_url_text_view);
        }

        @Override
        public void onClick(View v) {

        }
    }

}
