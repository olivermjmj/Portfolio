---
title: "RAG"
---

## What is a RAG bot
A Retrieval-Augmented Generation bot (RAG) is a technique that combines an LLM with an external knowledge base to make its answers more accurate, up to date and grounded in specific information.

This is done by giving the RAG bot access to specific resources that it can retrieve information from before answering a question. These resources can be different file types such as Markdown files, PDFs or other documents, but for my implementation I primarily use Markdown files (`.md`).

Instead of giving the LLM every document at once, the documents are split into smaller pieces called **chunks**. When a user asks a question, the RAG system searches through these chunks and retrieves the ones that are most relevant to the question.

The relevant chunks are then given to the LLM as additional context, which it can use when generating its answer.

## My implementation of a RAG
For creating and updating my RAG, I take my Markdown files and let Dify handle the initial chunking and knowledge base: [https://dify.ai/](https://dify.ai/)

Later on I plan to experiment with **Chonkie**: [https://github.com/feyninc/chonkie](https://github.com/feyninc/chonkie)

The reason for this is that I want more control over how my Markdown files are chunked and be able to compare whether a more specialized chunking strategy improves the quality of the retrieval compared to the default Dify setup.

For reranking I currently use **Jina AI**: [https://jina.ai/](https://jina.ai/)

The reason for using Jina AI is mainly that they offer a very generous free tier, which currently gives access to 10 million tokens. This makes it useful for experimenting with reranking without having to worry too much about cost while developing the RAG.

The reranker is used after the initial retrieval. Dify might for example retrieve several chunks that it believes are relevant to the user's question, where the reranker then looks at those results again and sorts them based on how relevant they actually are.

The flow therefore looks roughly like this:

`User question → Retrieval → Relevant chunks → Reranking → LLM → Answer`

Later on I would like to replace the hosted reranking model with something that I can self-host. This would give me more control over the system and remove the dependency on an external API for reranking, but for now Jina AI works well for what I need.