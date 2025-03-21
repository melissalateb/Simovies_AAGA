# Neurobot – Projet Simovies

## Description

Notre équipe, **Neurobot**, a mis en place des comportements distincts pour le robot principal et le robot secondaire afin d’assurer une stratégie d’équipe coordonnée lors des affrontements.

## Objectifs

- **Stratégie d’équipe unifiée :**  
  Fusionner plusieurs comportements pour le robot principal et le robot secondaire et adapter dynamiquement la stratégie selon l’appartenance à Team A ou Team B.

- **Navigation et combat :**  
  Détecter les ennemis via les capteurs radar, contourner les obstacles (wrecks) et ajuster la trajectoire en fonction des positions alliées et adverses.

- **Communication interne :**  
  Mettre en place un protocole de messages simple pour échanger les positions, états et commandes entre les robots.

## Organisation du projet

Le projet se compose principalement de deux fichiers de comportement :

1. **NeurobotMain.java** – Contient la logique et la stratégie du robot principal.
2. **NeurobotSecondary.java** – Regroupe la logique pour le robot secondaire, avec des branches spécifiques pour Team A et Team B.


## Fonctionnalités clés

- **Détection d’équipe :**  
  Le comportement du robot s’adapte en fonction du type (Main ou Secondary) et de l’équipe à laquelle il appartient.

- **Navigation adaptative :**  
  Le robot ajuste sa trajectoire pour contourner les obstacles et se repositionner en fonction des positions des ennemis et alliés, grâce à des méthodes dédiées.

- **Communication inter-robots :**  
  Un protocole de messages permet aux robots d’échanger des informations essentielles (coordonnées, états, commandes d’attaque) pour coordonner leur stratégie.

## Binôme

Ce projet a été réalisé en binôme avec **Darko DJORDJEVIC**.
