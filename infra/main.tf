terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  required_version = ">= 1.5"
}

provider "aws" {
  profile = "default"
  region  = "us-east-2"
}

resource "aws_vpc" "kottage_vpc" {
  cidr_block = "10.0.0.0/16"
}

resource "aws_subnet" "public" {
  count             = 2
  vpc_id            = aws_vpc.kottage_vpc.id
  cidr_block        = "10.0.${count.index * 2}.0/24"
  availability_zone = var.main_availability_zones[count.index]

  tags = {
    Name  = "public"
    Count = count.index
  }
}

resource "aws_internet_gateway" "i_gw" {
  vpc_id = aws_vpc.kottage_vpc.id

  tags = {
    Name = "i_gw"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.kottage_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.i_gw.id
  }

  tags = {
    Name = "public"
  }

  depends_on = [aws_internet_gateway.i_gw]
}

resource "aws_main_route_table_association" "public" {
  vpc_id         = aws_vpc.kottage_vpc.id
  route_table_id = aws_route_table.public.id
}
