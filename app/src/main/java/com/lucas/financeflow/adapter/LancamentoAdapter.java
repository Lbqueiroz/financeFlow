package com.lucas.financeflow.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lucas.financeflow.R;
import com.lucas.financeflow.data.model.Lancamento;

import java.util.ArrayList;
import java.util.List;

public class LancamentoAdapter extends RecyclerView.Adapter<LancamentoAdapter.LancamentoViewHolder> {

    private List<Lancamento> lancamentos = new ArrayList<>();
    private final OnLancamentoClickListener listener;

    public interface OnLancamentoClickListener {
        void onEditar(Lancamento lancamento);
        void onExcluir(Lancamento lancamento);
    }

    public LancamentoAdapter(OnLancamentoClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LancamentoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lancamento, parent, false);

        return new LancamentoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LancamentoViewHolder holder, int position) {
        Lancamento lancamento = lancamentos.get(position);

        holder.txtDescricao.setText(lancamento.descricao);
        holder.txtValor.setText("R$ " + String.format("%.2f", lancamento.valor));

        holder.txtDetalhes.setText(
                lancamento.tipo + " • " +
                        lancamento.categoria + " • " +
                        lancamento.conta + " • " +
                        lancamento.origemDestino
        );

        holder.btnEditar.setOnClickListener(v -> listener.onEditar(lancamento));
        holder.btnExcluir.setOnClickListener(v -> listener.onExcluir(lancamento));
    }

    @Override
    public int getItemCount() {
        return lancamentos.size();
    }

    public void setLancamentos(List<Lancamento> novaLista) {
        this.lancamentos = novaLista;
        notifyDataSetChanged();
    }

    static class LancamentoViewHolder extends RecyclerView.ViewHolder {

        TextView txtDescricao, txtValor, txtDetalhes;
        Button btnEditar, btnExcluir;

        public LancamentoViewHolder(@NonNull View itemView) {
            super(itemView);

            txtDescricao = itemView.findViewById(R.id.txtDescricaoItem);
            txtValor = itemView.findViewById(R.id.txtValorItem);
            txtDetalhes = itemView.findViewById(R.id.txtDetalhesItem);
            btnEditar = itemView.findViewById(R.id.btnEditarItem);
            btnExcluir = itemView.findViewById(R.id.btnExcluirItem);
        }
    }
}