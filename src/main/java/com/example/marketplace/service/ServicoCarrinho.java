package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();
        int totalQuantidadeItens = 0;
        int descontoCategoriaTotal = 0;

        // =========================
        // Monta os itens do carrinho e processa base de descontos
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            int quantidade = selecao.getQuantidade();
            itens.add(new ItemCarrinho(produto, quantidade));

            totalQuantidadeItens += quantidade;

            // CORREÇÃO 1: Extrai o nome do Enum CategoriaProduto como String
            String categoria = produto.getCategoria().name(); 
            
            int descontoItem = switch (categoria) {
                case "CAPINHA" -> 3;
                case "CARREGADOR" -> 5;
                case "FONE" -> 3;
                case "PELICULA" -> 2;
                case "SUPORTE" -> 2;
                default -> 0;
            };
            descontoCategoriaTotal += (descontoItem * quantidade);
        }

        // =========================
        // Consolida Percentual de Desconto
        // =========================
        int descontoQuantidade = 0;
        if (totalQuantidadeItens >= 4) {
            descontoQuantidade = 10;
        } else if (totalQuantidadeItens == 3) {
            descontoQuantidade = 7;
        } else if (totalQuantidadeItens == 2) {
            descontoQuantidade = 5;
        }

        int percentualDescontoInt = Math.min(descontoQuantidade + descontoCategoriaTotal, 25);
        
        // CORREÇÃO 2: Converte o percentual para BigDecimal para bater com o construtor
        BigDecimal percentualDesconto = BigDecimal.valueOf(percentualDescontoInt);

        // =========================
        // Calcula subtotal e valores finais
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // O cálculo do valor do desconto agora usa a variável BigDecimal
        BigDecimal valorDesconto = subtotal.multiply(percentualDesconto)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }
}